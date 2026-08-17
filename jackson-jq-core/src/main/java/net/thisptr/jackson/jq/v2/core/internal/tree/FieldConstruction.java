package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public interface FieldConstruction<JsonNode> {

	interface FieldConsumer<JsonNode> {
		void accept(String name, JsonNode value) throws JsonQueryException;
	}

	void evaluate(@Nullable StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException;
}
