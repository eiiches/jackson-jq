package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

public class ResolvedIndexFieldAccess extends ResolvedFieldAccess {
	private List<Long> indices;

	public ResolvedIndexFieldAccess(final boolean permissive, final List<Long> indices) {
		super(permissive);
		this.indices = indices;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final long _index : indices) {
			if (in.isArray()) {
				final long index = _index < 0 ? _index + in.size() : _index;
				if (0 <= index && index < in.size()) {
					out.add(in.get((int) index));
				} else {
					out.add(NullNode.getInstance());
				}
			} else if (in.isNull()) {
				out.add(NullNode.getInstance());
			} else {
				if (!permissive)
					throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
			}
		}
		return out;
	}

	public List<Long> indices() {
		return Collections.unmodifiableList(indices);
	}
}