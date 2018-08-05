package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Range;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedEmptyFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedIndexFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedRangeFieldAccess;
import net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved.ResolvedStringFieldAccess;

public class BracketFieldAccess extends FieldAccess {
	private Expression end;
	private Expression begin;
	private boolean isRange;

	public BracketFieldAccess(final Expression src, final Expression begin, final boolean permissive) {
		super(src, permissive);
		this.begin = begin;
		this.isRange = false;
	}

	public BracketFieldAccess(final Expression src, final Expression begin, final Expression end, final boolean permissive) {
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
			final List<JsonNode> accessorBeginTuple = new ArrayList<>();
			if (begin == null) {
				accessorBeginTuple.add(new IntNode(0));
			} else {
				begin.apply(scope, in, accessorBeginTuple::add);
			}

			final List<JsonNode> accessorEndTuple = new ArrayList<>();
			if (end == null) {
				accessorEndTuple.add(new IntNode(Integer.MAX_VALUE));
			} else {
				end.apply(scope, in, accessorEndTuple::add);
			}

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

			final List<JsonNode> accessorTuple = new ArrayList<>();
			begin.apply(scope, in, accessorTuple::add);
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
