package net.thisptr.jackson.jq.path;

import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class ArrayIndexOfPath<JsonNode> implements Path<JsonNode> {
	public final JsonNode subseq; // sub sequence to look for
	private final Path<JsonNode> parent;

	public static <JsonNode> ArrayIndexOfPath<JsonNode> chainIfNotNull(final Path<JsonNode> parent, final JsonNode subseq) {
		if (parent == null)
			return null;
		return new ArrayIndexOfPath<>(parent, subseq);
	}

	public ArrayIndexOfPath(final Path<JsonNode> parent, final JsonNode subseq) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (subseq == null)
			throw new NullPointerException("subseq must not be null");
		// Note: cannot validate isArray() without JsonProvider here
		this.subseq = subseq;
	}

	@Override
	public void toJsonNode(final JsonProvider<JsonNode> jsonProvider, final JsonNode out) throws JsonQueryException {
		parent.toJsonNode(jsonProvider, out);
		jsonProvider.add(out, subseq);
	}

	@Override
	public void get(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, boolean permissive) throws JsonQueryException {
		parent.get(jsonProvider, in, ipath, (parent, ppath) -> {
			resolve(jsonProvider, parent, ppath, output, subseq, permissive);
		}, permissive);
	}

	@Override
	public JsonNode mutate(final JsonProvider<JsonNode> jsonProvider, final JsonNode in, final Mutation<JsonNode> mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(jsonProvider, in, (oldval) -> {
			throw new JsonQueryException("Cannot update field at array index of array");
		}, makeParent);
	}

	private static <JsonNode> JsonNode indexOfAll(final JsonProvider<JsonNode> jsonProvider, final JsonNode seq, final JsonNode subseq) {
		final JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		JsonNode out = jsonProvider.createArray();

		if (jsonProvider.size(subseq) != 0) {
			shift: for (int i = 0; i < jsonProvider.size(seq) - jsonProvider.size(subseq) + 1; ++i) {
				for (int j = 0; j < jsonProvider.size(subseq); ++j)
					if (comparator.compare(jsonProvider.get(seq, i + j), jsonProvider.get(subseq, j)) != 0)
						continue shift;
				out = jsonProvider.add(out, jsonProvider.createNumber(i));
			}
		}

		return out;
	}

	public static <JsonNode> void resolve(final JsonProvider<JsonNode> jsonProvider, final JsonNode pobj, final Path<JsonNode> ppath, final PathOutput<JsonNode> output, final JsonNode subseq, final boolean permissive) throws JsonQueryException {
		assert jsonProvider.getNodeType(subseq) == JsonNodeType.ARRAY;
		if (jsonProvider.getNodeType(pobj) == JsonNodeType.ARRAY) {
			final JsonNode indexList = indexOfAll(jsonProvider, pobj, subseq);
			output.emit(indexList, ArrayIndexOfPath.chainIfNotNull(ppath, subseq));
		} else {
			if (!permissive)
				throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with array", jsonProvider.getNodeType(pobj));
		}
	}
}
