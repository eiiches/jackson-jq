package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ObjectConstruction<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final List<FieldConstruction<JsonNode>> fields = new ArrayList<>();

	public ObjectConstruction(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public void add(FieldConstruction<JsonNode> field) {
		fields.add(field);
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		Map<String, JsonNode> tmp = new LinkedHashMap<>(fields.size());
		applyRecursive(jsonProvider, frame, in, output, fields, tmp);
	}

	private static <JsonNode> void applyRecursive(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, PathOutput<JsonNode> output, List<FieldConstruction<JsonNode>> fields, Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.isEmpty()) {
			@Var JsonNode obj = jsonProvider.createObject();
			for (Entry<String, JsonNode> e : tmp.entrySet())
				obj = jsonProvider.set(obj, e.getKey(), e.getValue());
			output.emit(obj, null);
			return;
		}
		fields.get(0).evaluate(frame, in, (k, v) -> {
			tmp.put(k, v);
			applyRecursive(jsonProvider, frame, in, output, fields.subList(1, fields.size()), tmp);
			tmp.remove(k);
		});
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		@Var String sep = "";
		for (FieldConstruction<JsonNode> field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
