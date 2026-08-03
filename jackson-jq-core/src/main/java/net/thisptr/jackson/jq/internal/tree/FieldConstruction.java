package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface FieldConstruction {

	interface FieldConsumer {
		void accept(String name, JsonNode value) throws JsonQueryException;
	}

	void evaluate(Scope scope, JsonNode in, FieldConsumer consumer) throws JsonQueryException;
}
