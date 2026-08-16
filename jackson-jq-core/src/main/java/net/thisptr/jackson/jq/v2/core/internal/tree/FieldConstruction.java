package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public interface FieldConstruction<JsonNode> {

	interface FieldConsumer<JsonNode> {
		void accept(String name, JsonNode value) throws JsonQueryException;
	}

	void evaluate(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException;
}
