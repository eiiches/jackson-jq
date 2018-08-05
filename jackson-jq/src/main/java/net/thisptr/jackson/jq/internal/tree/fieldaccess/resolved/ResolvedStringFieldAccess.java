package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class ResolvedStringFieldAccess extends ResolvedFieldAccess {
	private List<String> keys;

	public List<String> keys() {
		return Collections.unmodifiableList(keys);
	}

	public ResolvedStringFieldAccess(final boolean permissive, final List<String> keys) {
		super(permissive);
		this.keys = keys;
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Output output) throws JsonQueryException {
		for (final String key : keys) {
			if (in.isNull()) {
				output.emit(NullNode.getInstance());
			} else if (in.isObject()) {
				final JsonNode n = in.get(key);
				output.emit(n == null ? NullNode.getInstance() : n);
			} else {
				if (!permissive)
					throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(in), key));
			}
		}
	}
}