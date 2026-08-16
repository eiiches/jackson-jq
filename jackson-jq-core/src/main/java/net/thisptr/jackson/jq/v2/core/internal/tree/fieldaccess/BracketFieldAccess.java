package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BracketFieldAccess extends FieldAccess {
	private Expression startExpr;
	private Expression endExpr = new NullLiteral();
	private boolean isRange;

	public BracketFieldAccess(Expression src, Expression atExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral();
		this.isRange = false;
	}

	public BracketFieldAccess(Expression src, Expression startExpr, Expression endExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral();
		this.endExpr = endExpr != null ? endExpr : new NullLiteral();
		this.isRange = true;
	}

	public Expression startExpr() {
		return startExpr;
	}

	public Expression endExpr() {
		return endExpr;
	}

	public boolean isRange() {
		return isRange;
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
	public <JsonNode> void apply(JsonProvider<JsonNode> jsonProvider, ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		if (isRange) {
			startExpr.apply(jsonProvider, frame, in, (start) -> {
				endExpr.apply(jsonProvider, frame, in, (end) -> {
					target.apply(jsonProvider, frame, in, path, (pobj, ppath) -> {
						JsonNodeType startType = jsonProvider.getNodeType(start);
						JsonNodeType endType = jsonProvider.getNodeType(end);
						if ((startType == JsonNodeType.NUMBER || startType == JsonNodeType.NULL) && (endType == JsonNodeType.NUMBER || endType == JsonNodeType.NULL)) {
							emitArrayRangeIndexPath(jsonProvider, permissive, start, end, pobj, ppath, output, requirePath);
						} else {
							if (!permissive)
								throw new JsonQueryTypeException(jsonProvider, "Start and end indices of an %s slice must be numbers", jsonProvider.getNodeType(pobj));
						}
					}, requirePath);
				});
			});
		} else { // isRange == false
			startExpr.apply(jsonProvider, frame, in, (accessor) -> {
				target.apply(jsonProvider, frame, in, path, (pobj, ppath) -> {
					JsonNodeType accessorType = jsonProvider.getNodeType(accessor);
					if (accessorType == JsonNodeType.NUMBER) {
						emitArrayIndexPath(jsonProvider, permissive, accessor, pobj, ppath, output, requirePath);
					} else if (accessorType == JsonNodeType.STRING) {
						emitObjectFieldPath(jsonProvider, permissive, jsonProvider.asText(accessor), pobj, ppath, output, requirePath);
					} else if (accessorType == JsonNodeType.ARRAY) {
						emitArrayIndexOfPath(jsonProvider, permissive, accessor, pobj, ppath, output, requirePath);
					} else {
						if (!permissive)
							throw new JsonQueryTypeException(jsonProvider, "Cannot index %s with %s", jsonProvider.getNodeType(pobj), accessorType);
					}
				}, requirePath);
			});
		}
	}
}
