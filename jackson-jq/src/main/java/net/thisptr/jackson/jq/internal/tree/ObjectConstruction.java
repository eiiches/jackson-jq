package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectConstruction extends JsonQuery {
	private List<FieldDef> fields = new ArrayList<>();

	private static class FieldDef {
		public final JsonQuery keyExpr;
		public final JsonQuery valueExpr;

		public FieldDef(final JsonQuery keyExpr, final JsonQuery valueExpr) {
			this.keyExpr = keyExpr;
			this.valueExpr = valueExpr;
		}

		public List<JsonNode> values(final Scope scope, final JsonNode in, final String key) throws JsonQueryException {
			if (valueExpr == null) {
				final JsonNode tmp = in.get(key);
				return Collections.singletonList(tmp == null ? NullNode.getInstance() : tmp);
			} else {
				return valueExpr.apply(scope, in);
			}
		}

		public List<String> keys(final Scope scope, final JsonNode in) throws JsonQueryException {
			final List<String> result = new ArrayList<>();
			for (final JsonNode keyNode : keyExpr.apply(scope, in)) {
				if (!keyNode.isTextual())
					throw new JsonQueryException("key must evaluate to string");
				result.add(keyNode.asText());
			}
			return result;
		}

		@Override
		public String toString() {
			String result = "(" + keyExpr.toString() + ")";
			if (valueExpr != null)
				return result + ": " + valueExpr;
			return result;
		}
	}

	public ObjectConstruction() {}

	public void addField(final JsonQuery keyExpr, final JsonQuery valueExpr) {
		fields.add(new FieldDef(keyExpr, valueExpr));
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		final Map<String, JsonNode> tmp = new HashMap<>();
		applyRecursive(scope, in, out, fields, tmp);
		return out;
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out, final List<FieldDef> fields, final Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.size() == 0) {
			final ObjectNode obj = scope.getObjectMapper().createObjectNode();
			for (final Entry<String, JsonNode> e : tmp.entrySet())
				obj.set(e.getKey(), e.getValue());
			out.add(obj);
			return;
		}

		final FieldDef def = fields.get(0);
		final List<String> keys = def.keys(scope, in);
		for (final String key : keys) {
			for (final JsonNode value : def.values(scope, in, key)) {
				tmp.put(key, value);
				applyRecursive(scope, in, out, fields.subList(1, fields.size()), tmp);
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("{");
		String sep = "";
		for (final FieldDef field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
