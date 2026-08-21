package net.thisptr.jackson.jq.v2.core.path;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ArrayIndexOfPath<JsonNode> implements Path<JsonNode> {
	public final JsonNode subseq; // sub sequence to look for
	private final Path<JsonNode> parent;
	private final @Nullable Version version;

	public static <JsonNode> @Nullable ArrayIndexOfPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode subseq) {
		return chainIfNotNull(parent, subseq, null);
	}

	public static <JsonNode> @Nullable ArrayIndexOfPath<JsonNode> chainIfNotNull(@Nullable Path<JsonNode> parent, JsonNode subseq, @Nullable Version version) {
		if (parent == null)
			return null;
		return new ArrayIndexOfPath<>(parent, subseq, version);
	}

	public ArrayIndexOfPath(Path<JsonNode> parent, JsonNode subseq) {
		this(parent, subseq, null);
	}

	public ArrayIndexOfPath(Path<JsonNode> parent, JsonNode subseq, @Nullable Version version) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (subseq == null)
			throw new NullPointerException("subseq must not be null");
		// Note: cannot validate isArray() without JsonProvider here
		this.subseq = subseq;
		this.version = version;
	}

	@Override
	public void toJsonNode(JsonProvider<JsonNode> jsonProvider, JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, subseq);
	}

	@Override
	public void get(JsonProvider<JsonNode> jsonProvider, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, subseq, permissive, version);
		}, permissive);
	}

	@Override
	public JsonNode mutate(JsonProvider<JsonNode> jsonProvider, JsonNode in, Mutation<JsonNode> mutation, boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			throw new JsonQueryException("Cannot update field at array index of array");
		}, makeParent);
	}

	private static <JsonNode> JsonNode indexOfAll(JsonProvider<JsonNode> jsonProvider, JsonNode seq, JsonNode subseq) {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		@Var JsonNode out = jsonProvider.createArray();

		if (jsonProvider.size(subseq) != 0) {
			shift: for (int i = 0; i < jsonProvider.size(seq) - jsonProvider.size(subseq) + 1; ++i) {
				for (int j = 0; j < jsonProvider.size(subseq); ++j)
					if (comparator.compare(jsonProvider.requireGet(seq, i + j), jsonProvider.requireGet(subseq, j)) != 0)
						continue shift;
				out = jsonProvider.add(out, jsonProvider.createNumber(i));
			}
		}

		return out;
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, JsonNode subseq, boolean permissive) throws JsonQueryException {
		resolve(jsonProvider, pobj, ppath, output, subseq, permissive, null);
	}

	public static <JsonNode> void resolve(JsonProvider<JsonNode> jsonProvider, JsonNode pobj, @Nullable Path<JsonNode> ppath, PathOutput<JsonNode> output, JsonNode subseq, boolean permissive, @Nullable Version version) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseq) == JsonNodeType.ARRAY;
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			JsonNode indexList = indexOfAll(jsonProvider, pobj, subseq);
			output.emit(indexList, ArrayIndexOfPath.chainIfNotNull(ppath, subseq, version));
		} else {
			if (!permissive)
				throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, pobj, subseq));
		}
	}
}
