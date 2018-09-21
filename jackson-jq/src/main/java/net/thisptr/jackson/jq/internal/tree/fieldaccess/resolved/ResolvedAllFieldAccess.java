package net.thisptr.jackson.jq.internal.tree.fieldaccess.resolved;

import java.util.ArrayList;
import java.util.Collections;
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

		final int size = in.size();

		if (in.isNull()) {
			if (!permissive)
				throw new JsonQueryException("Cannot iterate over null");
		} else if (in.isArray()) {
			switch (in.size()) {
				case 0:
					break;
				case 1:
					return Collections.singletonList(in.elements().next());
				default:
					final List<JsonNode> out = new ArrayList<>(size);
					for (JsonNode child : in) {
						out.add(child);
					}
					return out;
			}
		} else if (in.isObject()) {
			switch (in.size()) {
				case 0:
					break;
				case 1:
					return Collections.singletonList(in.fields().next().getValue());
				default:
					final List<JsonNode> out = new ArrayList<>(size);
					final Iterator<Entry<String, JsonNode>> fields = in.fields();
					while (fields.hasNext()) {
						out.add(fields.next().getValue());
					}
					return out;
			}
		} else {
			if (!permissive)
				throw JsonQueryException.format("Cannot iterate over %s", in.getNodeType());
		}

		return Collections.emptyList();
	}
}