package net.thisptr.jackson.jq.internal.tree.fieldaccess;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.tree.literal.NullLiteral;
import net.thisptr.jackson.jq.path.Path;

public class BracketFieldAccess extends FieldAccess {
	private Expression startExpr;
	private Expression endExpr;
	private boolean isRange;

	public BracketFieldAccess(final Expression src, final Expression atExpr, final boolean permissive) {
		super(src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral();
		this.isRange = false;
	}

	public BracketFieldAccess(final Expression src, final Expression startExpr, final Expression endExpr, final boolean permissive) {
		super(src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral();
		this.endExpr = endExpr != null ? endExpr : new NullLiteral();
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
	public void apply(final Scope scope, final JsonNode in, final Path path, final PathOutput output, final boolean requirePath) throws JsonQueryException {
		target.apply(scope, in, path, (pobj, ppath) -> {
			if (isRange) {
				startExpr.apply(scope, in, (start) -> {
					endExpr.apply(scope, in, (end) -> {
						if ((start.isNumber() || start.isNull()) && (end.isNumber() || end.isNull())) {
							emitArrayRangeIndexPath(permissive, start, end, pobj, ppath, output, requirePath);
						} else {
							if (!permissive)
								throw new JsonQueryTypeException("Start and end indices of an %s slice must be numbers", pobj.getNodeType());
						}
					});
				});
			} else { // isRange == false
				startExpr.apply(scope, in, (accessor) -> {
					if (accessor.isNumber()) {
						emitArrayIndexPath(permissive, accessor, pobj, ppath, output, requirePath);
					} else if (accessor.isTextual()) {
						emitObjectFieldPath(permissive, accessor.asText(), pobj, ppath, output, requirePath);
					} else {
						if (!permissive)
							throw new JsonQueryTypeException("Cannot index %s with %s", pobj.getNodeType(), accessor.getNodeType());
					}
				});
			}
		}, requirePath);
	}
}
