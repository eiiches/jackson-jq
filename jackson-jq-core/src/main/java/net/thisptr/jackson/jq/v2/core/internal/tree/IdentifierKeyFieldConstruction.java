package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final String key;
	public final @Nullable Expression<StackFrame, JsonNode> value;

	@Override
	public Cardinality getCardinality() {
		return value == null ? Cardinality.ONE : value.getCardinality();
	}

	public IdentifierKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, String key, @Nullable Expression<StackFrame, JsonNode> value) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
	}

	public IdentifierKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, String key) {
		this(jsonProvider, key, null);
	}

	// `{foo}` shorthand implicitly reads `in` when value is absent.
	@Override
	public boolean dependsOnInput() {
		return value == null || value.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return value != null && value.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(value);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(value);
	}

	@Override
	public void evaluate(StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		if (value == null) {
			consumer.accept(key, JsonNodeUtils.nullToNullNode(jsonProvider, jsonProvider.get(in, key)));
		} else {
			value.apply(frame, in, null, (v, opath) -> consumer.accept(key, v));
		}
	}

	@Override
	public String toString() {
		if (value == null) {
			return key;
		} else {
			return key + ": " + value.toString();
		}
	}
}
