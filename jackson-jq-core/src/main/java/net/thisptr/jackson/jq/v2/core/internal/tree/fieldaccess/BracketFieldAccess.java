package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BracketFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<StackFrame, JsonNode> startExpr;
	private Expression<StackFrame, JsonNode> endExpr;
	private boolean isRange;

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

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> atExpr, boolean permissive) {
		this(jsonProvider, src, atExpr, permissive, null);
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> atExpr, boolean permissive, @Nullable Version version) {
		super(jsonProvider, src, permissive, version);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral<>(jsonProvider);
		this.endExpr = new NullLiteral<>(jsonProvider);
		this.isRange = false;
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> startExpr, @Nullable Expression<StackFrame, JsonNode> endExpr, boolean permissive) {
		this(jsonProvider, src, startExpr, endExpr, permissive, null);
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> src, @Nullable Expression<StackFrame, JsonNode> startExpr, @Nullable Expression<StackFrame, JsonNode> endExpr, boolean permissive, @Nullable Version version) {
		super(jsonProvider, src, permissive, version);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral<>(jsonProvider);
		this.endExpr = endExpr != null ? endExpr : new NullLiteral<>(jsonProvider);
		this.isRange = true;
	}

	public Expression<StackFrame, JsonNode> startExpr() {
		return startExpr;
	}

	public Expression<StackFrame, JsonNode> endExpr() {
		return endExpr;
	}

	public boolean isRange() {
		return isRange;
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
	public String toString() {
		if (isRange) {
			return String.format("%s[%s : %s]%s", target, startExpr == null ? "" : startExpr, endExpr == null ? "" : endExpr, permissive ? "?" : "");
		} else {
			return String.format("%s[%s]%s", target, startExpr, permissive ? "?" : "");
		}
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		if (isRange) {
			startExpr.apply(frame, in, null, (start, opath) -> {
				endExpr.apply(frame, in, null, (end, opath2) -> {
					target.apply(frame, in, path, (pobj, ppath) -> {
						JsonNodeType startType = jsonProvider.getNodeType(start);
						JsonNodeType endType = jsonProvider.getNodeType(end);
						if ((startType == JsonNodeType.NUMBER || startType == JsonNodeType.NULL) && (endType == JsonNodeType.NUMBER || endType == JsonNodeType.NULL)) {
							emitArrayRangeIndexPath(jsonProvider, permissive, start, end, pobj, ppath, output, path != null, version);
						} else {
							if (!permissive)
								throw new JsonQueryTypeException(jsonProvider, version, "Start and end indices of an %s slice must be numbers", jsonProvider.getNodeType(pobj));
						}
					});
				});
			});
		} else { // isRange == false
			startExpr.apply(frame, in, null, (accessor, opath) -> {
				target.apply(frame, in, path, (pobj, ppath) -> {
					JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
					if (accessorType == JsonNodeType.NUMBER) {
						emitArrayIndexPath(jsonProvider, permissive, accessor, pobj, ppath, output, path != null, version);
					} else if (accessorType == JsonNodeType.STRING) {
						emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(accessor), pobj, ppath, output, path != null, version);
					} else if (accessorType == JsonNodeType.ARRAY) {
						emitArrayIndexOfPath(jsonProvider, permissive, accessor, pobj, ppath, output, path != null, version);
					} else {
						if (!permissive)
							throw new JsonQueryException(ExceptionMessages.cannotIndex(jsonProvider, version, pobj, accessor));
					}
				});
			});
		}
	}
}
