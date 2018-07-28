package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class ObjectConstruction extends JsonQuery {
	private final List<FieldConstruction> fields = new ArrayList<>();

	public ObjectConstruction() {}

	public void add(final FieldConstruction field) {
		fields.add(field);
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		final Map<String, JsonNode> tmp = new HashMap<>();
		applyRecursive(scope, in, out, fields, tmp);
		return out;
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out, final List<FieldConstruction> fields, final Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.size() == 0) {
			final ObjectNode obj = scope.getObjectMapper().createObjectNode();
			for (final Entry<String, JsonNode> e : tmp.entrySet())
				obj.set(e.getKey(), e.getValue());
			out.add(obj);
			return;
		}
		fields.get(0).evaluate(scope, in, (k, v) -> {
			tmp.put(k, v);
			applyRecursive(scope, in, out, fields.subList(1, fields.size()), tmp);
			tmp.remove(k);
		});
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("{");
		String sep = "";
		for (final FieldConstruction field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
