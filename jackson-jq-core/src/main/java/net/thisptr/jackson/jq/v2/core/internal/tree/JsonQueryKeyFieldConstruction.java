package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<StackFrame, JsonNode> key;
	private final Expression<StackFrame, JsonNode> value;
	private final @Nullable Version version;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(key.getCardinality(), value.getCardinality());
	}

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> key, Expression<StackFrame, JsonNode> value) {
		this(jsonProvider, key, value, null);
	}

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> key, Expression<StackFrame, JsonNode> value, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
	}

	public Expression<StackFrame, JsonNode> key() {
		return key;
	}

	public Expression<StackFrame, JsonNode> value() {
		return value;
	}

	@Override
	public boolean dependsOnInput() {
		return key.dependsOnInput() || value.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return key.dependsOnExternalState() || value.dependsOnExternalState();
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
				throw new JsonQueryTypeException(jsonProvider, version, "Cannot use %s as object key", k);
			value.apply(frame, in, UntrackedPath.getInstance(), (v, opath2) -> consumer.accept(jsonProvider.getString(k), v));
		});
	}

	@Override
	public String toString() {
		String result = "(" + key.toString() + ")";
		return result + ": " + value;
	}
}
