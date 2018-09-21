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
		if (keys.isEmpty()) {
			return Collections.emptyList();
		} else if (keys.size() == 1) {
			JsonNode node = apply0(in, keys.get(0));
			return node != null ? Collections.singletonList(node) : Collections.emptyList();
		} else {
			final List<JsonNode> out = new ArrayList<>(keys.size());
			for (String key : keys) {
				JsonNode node = apply0(in, key);
				if (node != null) {
					out.add(node);
				}
			}
			return out;
		}
	}

	private JsonNode apply0(final JsonNode in, final String key) throws JsonQueryException {
		if (in.isNull()) {
			return NullNode.getInstance();
		} else if (in.isObject()) {
			JsonNode n = in.get(key);
			return n == null ? NullNode.getInstance() : n;
		} else {
			if (!permissive) {
				throw new JsonQueryException(String.format("Cannot index %s with string \"%s\"", JsonNodeUtils.typeOf(in), key));
			} else {
				return null;
			}
		}
	}
}
