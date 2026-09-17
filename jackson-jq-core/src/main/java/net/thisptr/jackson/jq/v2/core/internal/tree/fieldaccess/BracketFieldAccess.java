package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.ValueLiteral;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class BracketFieldAccess<JsonNode> extends AbstractFieldAccess<JsonNode> {
	private final Expression<StackFrame, JsonNode> startExpr;
	private final Expression<StackFrame, JsonNode> endExpr;
	private final boolean isRange;
	private final int startOutputIndex;
	private final int endOutputIndex;

	@Override
	public Cardinality getCardinality() {
		if (isRange) {
			return !permissive
					? CardinalityUtils.multiply(target.getCardinality(), startExpr.getCardinality(), endExpr.getCardinality())
					: (target.getCardinality() == Cardinality.ZERO || startExpr.getCardinality() == Cardinality.ZERO || endExpr.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN);
		} else {
			return !permissive
					? CardinalityUtils.multiply(target.getCardinality(), startExpr.getCardinality())
					: (target.getCardinality() == Cardinality.ZERO || startExpr.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN);
		}
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> atExpr, boolean permissive, Version version, int targetOutputIndex, int startOutputIndex) {
		super(jsonProvider, src, permissive, version, targetOutputIndex);
		this.startExpr = atExpr != null ? atExpr : new ValueLiteral<>(jsonProvider.createNull());
		this.endExpr = new ValueLiteral<>(jsonProvider.createNull());
		this.isRange = false;
		this.startOutputIndex = startOutputIndex;
		this.endOutputIndex = Memory.NO_OUTPUT_COUNTER;
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> startExpr, @Nullable Expression<StackFrame, JsonNode> endExpr, boolean permissive, Version version, int targetOutputIndex, int startOutputIndex, int endOutputIndex) {
		super(jsonProvider, src, permissive, version, targetOutputIndex);
		this.startExpr = startExpr != null ? startExpr : new ValueLiteral<>(jsonProvider.createNull());
		this.endExpr = endExpr != null ? endExpr : new ValueLiteral<>(jsonProvider.createNull());
		this.isRange = true;
		this.startOutputIndex = startOutputIndex;
		this.endOutputIndex = endOutputIndex;
	}

	@Override
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		Expression<StackFrame, JsonNode> rewrittenTarget = rewriter.rewrite(target);
		Expression<StackFrame, JsonNode> rewrittenStart = rewriter.rewrite(startExpr);
		Expression<StackFrame, JsonNode> rewrittenEnd = isRange ? rewriter.rewrite(endExpr) : endExpr;
		if (rewrittenTarget == target && rewrittenStart == startExpr && rewrittenEnd == endExpr)
			return this;
		return isRange
				? new BracketFieldAccess<>(jsonProvider, rewrittenTarget, rewrittenStart, rewrittenEnd, permissive, version, targetOutputIndex, startOutputIndex, endOutputIndex)
				: new BracketFieldAccess<>(jsonProvider, rewrittenTarget, rewrittenStart, permissive, version, targetOutputIndex, startOutputIndex);
	}

	@Override
	public boolean dependsOnInput() {
		return super.dependsOnInput() || startExpr.dependsOnInput() || endExpr.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return super.dependsOnExternalState() || startExpr.dependsOnExternalState() || endExpr.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(target, startExpr, endExpr);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(target, startExpr, endExpr);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		if (isRange) {
			startExpr.apply(frame, in, UntrackedPath.getInstance(), (start, opath) -> {
				memory.countOutput(startOutputIndex);
				endExpr.apply(frame, in, UntrackedPath.getInstance(), (end, opath2) -> {
					memory.countOutput(endOutputIndex);
					target.apply(frame, in, path, (pobj, ppath) -> {
						memory.countOutput(targetOutputIndex);
						emitIndexRangePath(jsonProvider, permissive, start, end, pobj, ppath, output, !(path instanceof UntrackedPath), version);
					});
				});
			});
		} else { // isRange == false
			startExpr.apply(frame, in, UntrackedPath.getInstance(), (accessor, opath) -> {
				memory.countOutput(startOutputIndex);
				target.apply(frame, in, path, (pobj, ppath) -> {
					memory.countOutput(targetOutputIndex);
					JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
					if (accessorType == JsonNodeType.NUMBER) {
						emitArrayIndexPath(jsonProvider, permissive, accessor, pobj, ppath, output, !(path instanceof UntrackedPath), version);
					} else if (accessorType == JsonNodeType.STRING) {
						emitObjectFieldPath(jsonProvider, permissive, jsonProvider.getString(accessor), pobj, ppath, output, !(path instanceof UntrackedPath), version);
					} else if (accessorType == JsonNodeType.ARRAY) {
						emitIndexOfPath(jsonProvider, permissive, accessor, pobj, ppath, output, !(path instanceof UntrackedPath), version);
					} else {
						if (!permissive)
							throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, pobj, accessor));
					}
				});
			});
		}
	}
}
