package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface FieldConstruction<JsonNode> {

	interface FieldConsumer<JsonNode> {
		void accept(String name, JsonNode value) throws JsonQueryException;
	}

	void evaluate(Scope<JsonNode> scope, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException;
}
