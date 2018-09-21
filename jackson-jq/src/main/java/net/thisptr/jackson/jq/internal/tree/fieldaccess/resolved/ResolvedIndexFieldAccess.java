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

		int size = indices.size();

		if (in.isArray()) {
			switch (size) {
				case 0:
					break;
				case 1:
					return Collections.singletonList(apply0(in, indices.get(0)));
				default:
					final List<JsonNode> out = new ArrayList<>(size);
					for (final long _index : indices) {
						out.add(apply0(in, _index));
					}
					return out;
			}
		} else if (in.isNull()) {
			switch (size) {
				case 0:
					break;
				case 1:
					return Collections.singletonList(NullNode.getInstance());
				default:
					final List<JsonNode> out = new ArrayList<>(size);
					for (int i = 0; i < size; i++) {
						out.add(NullNode.getInstance());
					}
					return out;
			}
		} else {
			if (!permissive)
				throw JsonQueryException.format("Cannot index %s with number", in.getNodeType());
		}
		return Collections.emptyList();
	}

	private JsonNode apply0(final JsonNode in, long _index) {
		final long index = _index < 0 ? _index + in.size() : _index;
		if (0 <= index && index < in.size()) {
			return in.get((int) index);
		} else {
			return NullNode.getInstance();
		}
	}

	public List<Long> indices() {
		return Collections.unmodifiableList(indices);
	}
}