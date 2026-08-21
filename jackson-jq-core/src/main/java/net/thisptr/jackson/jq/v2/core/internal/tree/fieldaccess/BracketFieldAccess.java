package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BracketFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<JsonNode> startExpr;
	private Expression<JsonNode> endExpr;
	private boolean isRange;

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, @Nullable Expression<JsonNode> atExpr, boolean permissive) {
		this(jsonProvider, src, atExpr, permissive, null);
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, @Nullable Expression<JsonNode> atExpr, boolean permissive, @Nullable Version version) {
		super(jsonProvider, src, permissive, version);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral<>(jsonProvider);
		this.endExpr = new NullLiteral<>(jsonProvider);
		this.isRange = false;
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, @Nullable Expression<JsonNode> startExpr, @Nullable Expression<JsonNode> endExpr, boolean permissive) {
		this(jsonProvider, src, startExpr, endExpr, permissive, null);
	}

	public BracketFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<JsonNode> src, @Nullable Expression<JsonNode> startExpr, @Nullable Expression<JsonNode> endExpr, boolean permissive, @Nullable Version version) {
		super(jsonProvider, src, permissive, version);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral<>(jsonProvider);
		this.endExpr = endExpr != null ? endExpr : new NullLiteral<>(jsonProvider);
		this.isRange = true;
	}

	public Expression<JsonNode> startExpr() {
		return startExpr;
	}

	public Expression<JsonNode> endExpr() {
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
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		if (isRange) {
			startExpr.apply(frame, in, (start) -> {
				endExpr.apply(frame, in, (end) -> {
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
			startExpr.apply(frame, in, (accessor) -> {
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
							throw new JsonQueryException(JsonNodeUtils.cannotIndex(jsonProvider, version, pobj, accessor));
					}
				});
			});
		}
	}
}
