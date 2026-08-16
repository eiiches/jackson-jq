package net.thisptr.jackson.jq.v2.core.internal.tree;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class ResolvedVariableKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final String name;
	private final boolean isLocal;
	private final int slot;

	public ResolvedVariableKeyFieldConstruction(String name, boolean isLocal, int slot) {
		this.name = name;
		this.isLocal = isLocal;
		this.slot = slot;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void evaluate(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		@Var JsonNode value = null;
		if (frame != null) {
			if (isLocal) {
				value = frame.getValueNode(slot);
			} else {
				Closure<JsonNode> closure = frame.getClosure();
				if (closure != null) {
					Object raw = closure.getVariable(slot);
					if (raw instanceof ExecutionStack.PathAndValue) {
						value = ((ExecutionStack.PathAndValue<JsonNode>) raw).getValue();
					} else {
						value = (JsonNode) raw;
					}
				}
			}
		}
		consumer.accept(name, JsonNodeUtils.nullToNullNode(jsonProvider, value));
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
