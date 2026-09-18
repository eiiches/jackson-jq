package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class NegativeExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<StackFrame, JsonNode> value;
	private final Version version;
	private final int valueOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return value.getCardinality();
	}

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> value, Version version, int valueOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.value = value;
		this.version = version;
		this.valueOutputIndex = valueOutputIndex;
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
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		Expression<StackFrame, JsonNode> rewritten = rewriter.rewrite(value);
		return rewritten == value ? this : new NegativeExpression<>(jsonProvider, rewritten, version, valueOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		value.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> {
			memory.countOutput(valueOutputIndex);
			if (!jsonProvider.isNumber(v))
				throw new JsonQueryTypeException("%s cannot be negated", ExceptionMessages.describe(jsonProvider, version, v));
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, -jsonProvider.getNumberAsDoubleRounded(v)), UntrackedPath.getInstance());
		});
	}
}
