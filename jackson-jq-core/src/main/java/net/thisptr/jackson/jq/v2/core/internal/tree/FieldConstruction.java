package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public interface FieldConstruction<JsonNode> extends FreeVariables {

	default Cardinality getCardinality() {
		return Cardinality.UNKNOWN;
	}

	interface FieldConsumer<JsonNode> {
		void accept(String name, JsonNode value) throws JsonQueryException;
	}

	default boolean dependsOnInput() {
		return true;
	}

	default boolean dependsOnExternalState() {
		return true;
	}

	@Override
	default Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	@Override
	default boolean hasOpaqueVariableReference() {
		return true;
	}

	void evaluate(StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException;
}
