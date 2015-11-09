package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Range;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedEmptyFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedIndexFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedRangeFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

public class BracketFieldAccess extends FieldAccess {
	private JsonQuery end;
	private JsonQuery begin;
	private boolean isRange;

	public BracketFieldAccess(final JsonQuery src, final JsonQuery begin, final boolean permissive) {
		super(src, permissive);
		this.begin = begin;
		this.isRange = false;
	}

	public BracketFieldAccess(final JsonQuery src, final JsonQuery begin, final JsonQuery end, final boolean permissive) {
		super(src, permissive);
		this.begin = begin;
		this.end = end;
		this.isRange = true;
	}

	@Override
	public String toString() {
		if (isRange) {
			return String.format("%s[%s : %s]%s", target, begin == null ? "" : begin, end == null ? "" : end, permissive ? "?" : "");
		} else {
			return String.format("%s[%s]%s", target, begin, permissive ? "?" : "");
		}
	}

	@Override
	public ResolvedFieldAccess resolveFieldAccess(final Scope scope, final JsonNode in) throws JsonQueryException {
		if (isRange) {
			final List<JsonNode> accessorBeginTuple = begin == null ? Collections.singletonList((JsonNode) new IntNode(0)) : begin.apply(scope, in);
			final List<JsonNode> accessorEndTuple = end == null ? Collections.singletonList((JsonNode) new IntNode(Integer.MAX_VALUE)) : end.apply(scope, in);
			final List<Range> ranges = new ArrayList<>();
			for (final JsonNode accessorBegin : accessorBeginTuple) {
				for (final JsonNode accessorEnd : accessorEndTuple) {
					if (JsonNodeUtils.isIntegralNumber(accessorBegin) && JsonNodeUtils.isIntegralNumber(accessorEnd)) {
						final long indexBegin = accessorBegin.asLong();
						final long indexEnd = accessorEnd.asLong();
						ranges.add(new Range(indexBegin, indexEnd));
					} else {
						if (!permissive)
							throw JsonQueryException.format("Start and end indices of an %s slice must be numbers", in.getNodeType());
						return new ResolvedEmptyFieldAccess(permissive);
					}
				}
			}
			return new ResolvedRangeFieldAccess(permissive, ranges);
		} else { // isRange == false
			final List<Long> indices = new ArrayList<>();
			final List<String> keys = new ArrayList<>();

			final List<JsonNode> accessorTuple = begin.apply(scope, in);
			for (final JsonNode accessor : accessorTuple) {
				if (JsonNodeUtils.isIntegralNumber(accessor)) {
					final long index = accessor.asLong();
					indices.add(index);
				} else if (accessor.isTextual()) {
					final String key = accessor.asText();
					keys.add(key);
				} else {
					if (!permissive)
						throw JsonQueryException.format("Cannot index %s with %s", in.getNodeType(), accessor.getNodeType());
					return new ResolvedEmptyFieldAccess(permissive);
				}
			}

			if (!indices.isEmpty() && !keys.isEmpty())
				throw new JsonQueryException("bad index");
			if (!indices.isEmpty())
				return new ResolvedIndexFieldAccess(permissive, indices);
			if (!keys.isEmpty())
				return new ResolvedStringFieldAccess(permissive, keys);
			return new ResolvedEmptyFieldAccess(permissive);
		}
	}
}
