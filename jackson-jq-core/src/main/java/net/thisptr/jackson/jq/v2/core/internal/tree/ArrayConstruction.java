package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ArrayConstruction implements Expression {
	public final @Nullable Expression q;

	public ArrayConstruction() {
		this(null);
	}

	public ArrayConstruction(@Nullable Expression q) {
		this.q = q;
	}

	@Override
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		JsonNode[] array = (JsonNode[]) new Object[] { jsonProvider.createArray() };
		if (q != null)
			q.apply(jsonProvider, frame, in, (out) -> array[0] = jsonProvider.add(array[0], out));
		output.emit(array[0], null);
	}

	@Override
	public String toString() {
		if (q == null)
			return "[]";
		return String.format("[%s]", q);
	}
}
