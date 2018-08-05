package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class ResolvedAllFieldAccess extends ResolvedFieldAccess {
	public ResolvedAllFieldAccess(final boolean permissive) {
		super(permissive);
	}

	@Override
	public void apply(Scope scope, JsonNode in, final Output output) throws JsonQueryException {
		if (in.isNull()) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null");
		} else if (in.isArray()) {
			final Iterator<JsonNode> values = in.iterator();
			while (values.hasNext())
				output.emit(values.next());
		} else if (in.isObject()) {
			final Iterator<Entry<String, JsonNode>> fields = in.fields();
			while (fields.hasNext())
				output.emit(fields.next().getValue());
		} else {
			if (!permissive)
				throw JsonQueryException.format("Cannot iterate over %s", in.getNodeType());
		}
	}
}