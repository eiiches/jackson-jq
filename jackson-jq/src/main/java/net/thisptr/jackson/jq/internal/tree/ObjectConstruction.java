package net.thisptr.jackson.jq.internal.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class ObjectConstruction<JsonNode> implements Expression<JsonNode> {
	public final List<FieldConstruction<JsonNode>> fields = new ArrayList<>();

	public ObjectConstruction() {}

	public void add(final FieldConstruction<JsonNode> field) {
		fields.add(field);
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final Map<String, JsonNode> tmp = new LinkedHashMap<>(fields.size());
		applyRecursive(scope, in, output, fields, tmp);
	}

	private static <JsonNode> void applyRecursive(final Scope<JsonNode> scope, final JsonNode in, final PathOutput<JsonNode> output, final List<FieldConstruction<JsonNode>> fields, final Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.isEmpty()) {
			final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
			JsonNode obj = jsonProvider.createObject();
			for (final Entry<String, JsonNode> e : tmp.entrySet())
				obj = jsonProvider.set(obj, e.getKey(), e.getValue());
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
		for (final FieldConstruction<JsonNode> field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
