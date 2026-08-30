package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class StringKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final Expression<StackFrame, JsonNode> key;
	public final @Nullable Expression<StackFrame, JsonNode> value;
	private final Version version;

	@Override
	public Cardinality getCardinality() {
		return value == null ? key.getCardinality() : CardinalityUtils.multiply(key.getCardinality(), value.getCardinality());
	}

	public StringKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> key, @Nullable Expression<StackFrame, JsonNode> value, Version version) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
	}

	// `{(key)}` shorthand implicitly reads `in` when value is absent.
	@Override
	public boolean dependsOnInput() {
		return key.dependsOnInput() || value == null || value.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return key.dependsOnExternalState() || (value != null && value.dependsOnExternalState());
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(key, value);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(key, value);
	}

	@Override
	public void evaluate(StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		key.apply(frame, in, UntrackedPath.getInstance(), (k, opath) -> {
			if (jsonProvider.getNodeType(k) != JsonNodeType.STRING)
				throw new JsonQueryException("key must evaluate to string");
			String keyStr = jsonProvider.asString(k);
			if (value == null) {
				PathOperations.resolveObjectField(jsonProvider, in, UntrackedPath.getInstance(), (v, path) -> consumer.accept(keyStr, v), keyStr, false, version);
			} else {
				value.apply(frame, in, UntrackedPath.getInstance(), (v, opath2) -> consumer.accept(keyStr, v));
			}
		});
	}

	@Override
	public String toString() {
		if (value == null) {
			return key.toString();
		} else {
			return key.toString() + ": " + value.toString();
		}
	}
}
