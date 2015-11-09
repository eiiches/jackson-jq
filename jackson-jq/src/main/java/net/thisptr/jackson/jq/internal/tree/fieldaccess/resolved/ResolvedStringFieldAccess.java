package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

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
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final String key : keys) {
			if (in.isNull()) {
				out.add(NullNode.getInstance());
			} else if (in.isObject()) {
				final JsonNode n = in.get(key);
				out.add(n == null ? NullNode.getInstance() : n);
			} else {
				if (!permissive)
					throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(in), key));
			}
		}
		return out;
	}
}