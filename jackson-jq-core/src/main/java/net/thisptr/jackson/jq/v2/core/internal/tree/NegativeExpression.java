package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.math.BigDecimal;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.NumberType;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class NegativeExpression<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final AnalyzedExpression<JsonNode> value;
	private final Version version;
	private final int valueOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return value.getCardinality();
	}

	public NegativeExpression(JsonProvider<JsonNode> jsonProvider, AnalyzedExpression<JsonNode> value, Version version, int valueOutputIndex) {
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
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> rewritten = rewriter.rewrite(value);
		return rewritten == value ? this : new NegativeExpression<>(jsonProvider, rewritten, version, valueOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		value.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> {
			memory.countOutput(valueOutputIndex);
			if (!jsonProvider.isNumber(v))
				throw new JsonQueryTypeException("%s cannot be negated", ExceptionMessages.describe(jsonProvider, version, v));
			if (version.compareTo(Versions.JQ_1_8_0) >= 0) {
				// Only a number still carrying its decimal literal negates to +0; one this library computed
				// negates as a double, so its zero comes out as -0. BIG_DECIMAL and UNKNOWN are what a parsed
				// number reports -- the same pair the formatter prints by literal text -- whereas an int, long
				// or BigInteger node can only have been computed.
				NumberType type = jsonProvider.getNumberType(v);
				if (type == NumberType.BIG_DECIMAL || type == NumberType.UNKNOWN) {
					@Nullable BigDecimal exact = jsonProvider.getNumberAsBigDecimalExact(v);
					if (exact != null) {
						output.emit(jsonProvider.createNumber(exact.negate()), UntrackedPath.getInstance());
						return;
					}
				}
			}
			output.emit(JsonNodeUtils.asNumericNode(jsonProvider, -jsonProvider.getNumberAsDoubleRounded(v)), UntrackedPath.getInstance());
		});
	}
}
