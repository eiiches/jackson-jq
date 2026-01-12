package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.path.Path;

public class BracketFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<JsonNode> startExpr;
	private Expression<JsonNode> endExpr;
	private boolean isRange;

	public BracketFieldAccess(final Expression<JsonNode> src, final Expression<JsonNode> atExpr, final boolean permissive) {
		super(src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral<>();
		this.isRange = false;
	}

	public BracketFieldAccess(final Expression<JsonNode> src, final Expression<JsonNode> startExpr, final Expression<JsonNode> endExpr, final boolean permissive) {
		super(src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral<>();
		this.endExpr = endExpr != null ? endExpr : new NullLiteral<>();
		this.isRange = true;
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
	public void apply(final Scope<JsonNode> scope, final JsonNode in, final Path<JsonNode> path, final PathOutput<JsonNode> output, final boolean requirePath) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (isRange) {
			startExpr.apply(scope, in, (start) -> {
				endExpr.apply(scope, in, (end) -> {
					target.apply(scope, in, path, (pobj, ppath) -> {
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
			startExpr.apply(scope, in, (accessor) -> {
				target.apply(scope, in, path, (pobj, ppath) -> {
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
