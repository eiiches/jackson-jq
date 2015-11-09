package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public class ResolvedAllFieldAccess extends ResolvedFieldAccess {
	public ResolvedAllFieldAccess(final boolean permissive) {
		super(permissive);
	}

	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		if (in.isNull()) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null");
		} else if (in.isArray()) {
			final Iterator<JsonNode> values = in.iterator();
			while (values.hasNext())
				out.add(values.next());
		} else if (in.isObject()) {
			final Iterator<Entry<String, JsonNode>> fields = in.fields();
			while (fields.hasNext())
				out.add(fields.next().getValue());
		} else {
			if (!permissive)
				throw JsonQueryException.format("Cannot iterate over %s", in.getNodeType());
		}
		return out;
	}
}