package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class JsonQueryKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<StackFrame, JsonNode> key;
	private final Expression<StackFrame, JsonNode> value;
	private final Version version;
	private final int keyOutputIndex;
	private final int valueOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(key.getCardinality(), value.getCardinality());
	}

	public JsonQueryKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> key, Expression<StackFrame, JsonNode> value, Version version, int keyOutputIndex, int valueOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
		this.keyOutputIndex = keyOutputIndex;
		this.valueOutputIndex = valueOutputIndex;
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
		Memory memory = frame.getEnclosingMemory();
		key.apply(frame, in, UntrackedPath.getInstance(), (k, opath) -> {
			memory.countOutput(keyOutputIndex);
			if (!jsonProvider.isString(k))
				throw new JsonQueryTypeException("Cannot use %s as object key", ExceptionMessages.describe(jsonProvider, version, k));
			value.apply(frame, in, UntrackedPath.getInstance(), (v, opath2) -> {
				memory.countOutput(valueOutputIndex);
				consumer.accept(jsonProvider.getString(k), v);
			});
		});
	}
}
