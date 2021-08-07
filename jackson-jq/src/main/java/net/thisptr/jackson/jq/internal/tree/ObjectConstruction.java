package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class ObjectConstruction implements Expression {
	private final List<FieldConstruction> fields = new ArrayList<>();

	public ObjectConstruction() {}

	public void add(final FieldConstruction field) {
		fields.add(field);
	}

	@Override
	public void apply(final Scope scope, final JsonNode in, final Path ipath, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		final Map<String, JsonNode> tmp = new LinkedHashMap<>(fields.size());
		applyRecursive(scope, in, output, fields, tmp);
	}

	private static void applyRecursive(final Scope scope, final JsonNode in, final PathOutput output, final List<FieldConstruction> fields, final Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.isEmpty()) {
			final ObjectNode obj = scope.getObjectMapper().createObjectNode();
			for (final Entry<String, JsonNode> e : tmp.entrySet())
				obj.set(e.getKey(), e.getValue());
			output.emit(obj, null);
			return;
		}
		fields.get(0).evaluate(scope, in, (k, v) -> {
			tmp.put(k, v);
			applyRecursive(scope, in, output, fields.subList(1, fields.size()), tmp);
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
