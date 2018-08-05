package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class ResolvedIndexFieldAccess extends ResolvedFieldAccess {
	private List<Long> indices;

	public ResolvedIndexFieldAccess(final boolean permissive, final List<Long> indices) {
		super(permissive);
		this.indices = indices;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		for (final long _index : indices) {
			if (in.isArray()) {
				final long index = _index < 0 ? _index + in.size() : _index;
				if (0 <= index && index < in.size()) {
					output.emit(in.get((int) index));
				} else {
					output.emit(NullNode.getInstance());
				}
			} else if (in.isNull()) {
				output.emit(NullNode.getInstance());
			} else {
				if (!permissive)
					throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
			}
		}
	}

	public List<Long> indices() {
		return Collections.unmodifiableList(indices);
	}
}