package net.thisptr.jackson.jq.path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public class ArrayIndexOfPath implements Path {
	public final JsonNode subseq; // sub sequence to look for
	private final Path parent;

	public static ArrayIndexOfPath chainIfNotNull(final Path parent, final JsonNode subseq) {
		if (parent == null)
			return null;
		return new ArrayIndexOfPath(parent, subseq);
	}

	public ArrayIndexOfPath(final Path parent, final JsonNode subseq) {
		if (parent == null)
			throw new NullPointerException("parent must not be null");
		this.parent = parent;
		if (subseq == null)
			throw new NullPointerException("subseq must not be null");
		if (!subseq.isArray())
			throw new IllegalArgumentException("subseq must be an array ");
		this.subseq = subseq;
	}

	@Override
	public void toJsonNode(final ArrayNode out) throws JsonQueryException {
		parent.toJsonNode(out);
		out.add(subseq);
	}

	@Override
	public void get(final JsonNode in, final Path ipath, final PathOutput output, boolean permissive) throws JsonQueryException {
		parent.get(in, ipath, (parent, ppath) -> {
			resolve(parent, ppath, output, subseq, permissive);
		}, permissive);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public JsonNode mutate(final JsonNode in, final Mutation mutation, final boolean makeParent) throws JsonQueryException {
		return parent.mutate(in, (oldval) -> {
			throw new JsonQueryException("Cannot update field at array index of array");
		}, makeParent);
	}

	private static ArrayNode indexOfAll(final JsonNode seq, final JsonNode subseq) {
		final JsonNodeComparator comparator = JsonNodeComparator.getInstance();
		final ArrayNode out = MAPPER.createArrayNode();

		if (subseq.size() != 0) {
			shift: for (int i = 0; i < seq.size() - subseq.size() + 1; ++i) {
				for (int j = 0; j < subseq.size(); ++j)
					if (comparator.compare(seq.get(i + j), subseq.get(j)) != 0)
						continue shift;
				out.add(IntNode.valueOf(i));
			}
		}

		return out;
	}

	public static void resolve(final JsonNode pobj, final Path ppath, final PathOutput output, final JsonNode subseq, final boolean permissive) throws JsonQueryException {
		assert subseq.isArray();
		if (pobj.isArray()) {
			final ArrayNode indexList = indexOfAll(pobj, subseq);
			output.emit(indexList, ArrayIndexOfPath.chainIfNotNull(ppath, subseq));
		} else {
			if (!permissive)
				throw new JsonQueryTypeException("Cannot index %s with array", pobj.getNodeType());
		}
	}
}
