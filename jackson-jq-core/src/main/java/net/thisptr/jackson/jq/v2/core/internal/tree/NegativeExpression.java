package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class NegativeExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private Expression<StackFrame, JsonNode> value;
	private final @Nullable Version version;

	@Override
	public Cardinality getCardinality() {
		return value.getCardinality();
	}

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> value) {
		this(jsonProvider, value, null);
	}

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> value, @Nullable Version version) {
		this.jsonProvider = jsonProvider;
		this.value = value;
		this.version = version;
	}

	public Expression<StackFrame, JsonNode> value() {
		return value;
	}

	@Override
	public boolean dependsOnInput() {
		return value.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return value.dependsOnExternalState();
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
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		value.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> {
			if (!jsonProvider.isNumber(v))
				throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be negated", v);
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, -jsonProvider.getNumberAsDoubleRounded(v)), UntrackedPath.getInstance());
		});
	}

	@Override
	public String toString() {
		return "-(" + value.toString() + ")";
	}
}
