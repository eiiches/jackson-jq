package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Range;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "delpaths", nargs = 1)
public class DelPathsFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(args.get(0).getCardinality()).build((frame, in, ipath, output) -> {
			args.get(0).apply(frame, in, null, (paths, opath) -> {
				if (jsonProvider.getNodeType(paths) != JsonNodeType.ARRAY)
					throw new JsonQueryException("Paths must be specified as an array");

				List<List<JsonNode>> pathList = new ArrayList<>(jsonProvider.size(paths));
				for (JsonNode path : jsonProvider.iterate(paths)) {
					if (jsonProvider.getNodeType(path) != JsonNodeType.ARRAY)
						throw new JsonQueryException("Path must be specified as array, not " + JsonNodeUtils.typeOf(jsonProvider, path));
					pathList.add(JsonNodeUtils.asArrayList(jsonProvider, path));
				}

				// delpaths([[]]) (e.g. del(.)): an empty path deletes the whole input, and wins over
				// any sibling path in the same call, since there is no parent to omit it from.
				for (List<JsonNode> path : pathList) {
					if (path.isEmpty()) {
						output.emit(jsonProvider.createNull(), null);
						return;
					}
				}

				output.emit(delete(jsonProvider, in, pathList, 0, version), null);
			});
		});
	}

	/**
	 * Deletes, from {@code in}, every path in {@code paths} whose first {@code depth} segments have
	 * already been consumed by the caller. {@code in} always exists (it is either the original input,
	 * or a child fetched from a real, present key/index by the caller), so unlike {@link Path#mutate},
	 * this never has to special-case a synthetic "not created yet" value.
	 */
	private static <JsonNode> JsonNode delete(JsonProvider<JsonNode> jsonProvider, JsonNode in, List<List<JsonNode>> paths, int depth, Version version) throws JsonQueryException {
		JsonNodeType inType = jsonProvider.getNodeType(in);
		if (inType == JsonNodeType.NULL || paths.isEmpty())
			return in;

		List<List<JsonNode>> stringPathSegments = new ArrayList<>();
		List<List<JsonNode>> numberPathSegments = new ArrayList<>();
		List<List<JsonNode>> rangePathSegments = new ArrayList<>();
		for (List<JsonNode> path : paths) {
			JsonNode pathSegment = path.get(depth);
			switch (jsonProvider.getNodeType(pathSegment)) {
				case STRING:
					stringPathSegments.add(path);
					break;
				case NUMBER:
					numberPathSegments.add(path);
					break;
				case OBJECT:
					rangePathSegments.add(path);
					break;
				case ARRAY:
					throw new JsonQueryException("Cannot update field at array index of array");
				default:
					throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, inType, pathSegment));
			}
		}

		if (!stringPathSegments.isEmpty() && inType != JsonNodeType.OBJECT)
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, inType, stringPathSegments.get(0).get(depth)));

		if (!numberPathSegments.isEmpty() && inType != JsonNodeType.ARRAY)
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, inType, numberPathSegments.get(0).get(depth)));

		if (!rangePathSegments.isEmpty() && inType != JsonNodeType.ARRAY) {
			if (inType == JsonNodeType.STRING)
				throw new JsonQueryException("Cannot update field at object index of string");
			throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, inType, rangePathSegments.get(0).get(depth)));
		}

		if (!stringPathSegments.isEmpty())
			return deleteFromObject(jsonProvider, in, stringPathSegments, depth, version);
		return deleteFromArray(jsonProvider, in, numberPathSegments, rangePathSegments, depth, version);
	}

	private static <JsonNode> JsonNode deleteFromObject(JsonProvider<JsonNode> jsonProvider, JsonNode in, List<List<JsonNode>> paths, int depth, Version version) throws JsonQueryException {
		Set<String> deleteKeys = new HashSet<>();
		Map<String, List<List<JsonNode>>> recurseKeys = new LinkedHashMap<>();
		for (List<JsonNode> path : paths) {
			String key = jsonProvider.asText(path.get(depth));
			if (depth == path.size() - 1)
				deleteKeys.add(key);
			else
				recurseKeys.computeIfAbsent(key, k -> new ArrayList<>()).add(path);
		}

		@Var JsonNode out = jsonProvider.createObject();
		Iterator<Map.Entry<String, JsonNode>> iter = jsonProvider.fields(in);
		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> entry = iter.next();
			String key = entry.getKey();
			if (deleteKeys.contains(key))
				continue;
			List<List<JsonNode>> sub = recurseKeys.get(key);
			out = jsonProvider.set(out, key, sub == null ? entry.getValue() : delete(jsonProvider, entry.getValue(), sub, depth + 1, version));
		}
		return out;
	}

	private static <JsonNode> JsonNode deleteFromArray(JsonProvider<JsonNode> jsonProvider, JsonNode in, List<List<JsonNode>> numberPaths, List<List<JsonNode>> rangePaths, int depth, Version version) throws JsonQueryException {
		int size = jsonProvider.size(in);

		Set<Integer> deleteIndices = new HashSet<>();
		Map<Integer, List<List<JsonNode>>> recurseIndices = new LinkedHashMap<>();
		for (List<JsonNode> path : numberPaths) {
			JsonNode indexNode = path.get(depth);
			boolean terminal = depth == path.size() - 1;
			double raw = jsonProvider.asDouble(indexNode);
			if (terminal && raw < 0 && version.compareTo(Versions.JQ_1_5) <= 0) {
				// jq-1.5: [1,2,[1,3]]|delpaths([[-1,1]]) #=> [1,2,[1]]
				// jq-1.5: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2,[1,3]]
				// jq-master: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2]
				continue;
			}
			if (Double.isNaN(raw) || Double.isInfinite(raw))
				throw new JsonQueryException("Cannot use " + (Double.isNaN(raw) ? "nan" : "infinite") + " as array index");
			int index = (int) raw;
			int resolved = index < 0 ? index + size : index;
			if (resolved < 0 || resolved >= size)
				continue;
			if (terminal)
				deleteIndices.add(resolved);
			else
				recurseIndices.computeIfAbsent(resolved, k -> new ArrayList<>()).add(path);
		}

		List<Range> deleteRanges = new ArrayList<>();
		for (List<JsonNode> path : rangePaths) {
			if (depth != path.size() - 1)
				throw new JsonQueryException("Cannot index further into an array slice");
			JsonNode rangeNode = path.get(depth);
			deleteRanges.add(Range.resolve(jsonProvider, sliceBound(jsonProvider, rangeNode, "start"), sliceBound(jsonProvider, rangeNode, "end"), size));
		}

		@Var JsonNode out = jsonProvider.createArray();
		for (int i = 0; i < size; ++i) {
			if (deleteIndices.contains(i) || inAnyRange(deleteRanges, i))
				continue;
			List<List<JsonNode>> sub = recurseIndices.get(i);
			out = jsonProvider.add(out, sub == null ? jsonProvider.requireGet(in, i) : delete(jsonProvider, jsonProvider.requireGet(in, i), sub, depth + 1, version));
		}
		return out;
	}

	private static boolean inAnyRange(List<Range> ranges, int index) {
		for (Range range : ranges)
			if (index >= range.start && index < range.end)
				return true;
		return false;
	}

	private static <JsonNode> JsonNode sliceBound(JsonProvider<JsonNode> jsonProvider, JsonNode rangeNode, String field) throws JsonQueryException {
		JsonNode value = jsonProvider.requireGet(rangeNode, field);
		JsonNodeType type = jsonProvider.getNodeType(value);
		if (type != JsonNodeType.NUMBER && type != JsonNodeType.NULL)
			throw new JsonQueryException("Start and end indices of an array slice must be numbers");
		return value;
	}
}
