package net.thisptr.jackson.jq.v2.core.internal.ast.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.tree.literal.NullLiteral;

public class BracketFieldAccess extends FieldAccess {
	private AstNode startExpr;
	private AstNode endExpr = new NullLiteral();
	private boolean isRange;

	public BracketFieldAccess(AstNode src, AstNode atExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteral();
		this.isRange = false;
	}

	public BracketFieldAccess(AstNode src, AstNode startExpr, AstNode endExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteral();
		this.endExpr = endExpr != null ? endExpr : new NullLiteral();
		this.isRange = true;
	}

	public AstNode startExpr() {
		return startExpr;
	}

	public AstNode endExpr() {
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
}
