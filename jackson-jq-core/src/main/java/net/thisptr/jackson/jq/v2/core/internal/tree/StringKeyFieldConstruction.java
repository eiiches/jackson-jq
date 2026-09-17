package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class StringKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final Expression<StackFrame, JsonNode> key;
	public final @Nullable Expression<StackFrame, JsonNode> value;
	private final Version version;
	private final int keyOutputIndex;
	private final int valueOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return value == null ? key.getCardinality() : CardinalityUtils.multiply(key.getCardinality(), value.getCardinality());
	}

	public StringKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> key, @Nullable Expression<StackFrame, JsonNode> value, Version version, int keyOutputIndex, int valueOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
		this.keyOutputIndex = keyOutputIndex;
		this.valueOutputIndex = valueOutputIndex;
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
		Memory memory = frame.getEnclosingMemory();
		key.apply(frame, in, UntrackedPath.getInstance(), (k, opath) -> {
			memory.countOutput(keyOutputIndex);
			if (!jsonProvider.isString(k))
				throw new JsonQueryException("key must evaluate to string");
			String keyStr = jsonProvider.getString(k);
			if (value == null) {
				PathOperations.resolveObjectField(jsonProvider, in, UntrackedPath.getInstance(), (v, path) -> consumer.accept(keyStr, v), keyStr, false, version);
			} else {
				value.apply(frame, in, UntrackedPath.getInstance(), (v, opath2) -> {
					memory.countOutput(valueOutputIndex);
					consumer.accept(keyStr, v);
				});
			}
		});
	}
}
