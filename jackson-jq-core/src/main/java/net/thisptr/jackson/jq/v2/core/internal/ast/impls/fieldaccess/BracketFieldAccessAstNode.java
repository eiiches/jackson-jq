package net.thisptr.jackson.jq.v2.core.internal.ast.impls.fieldaccess;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal.NullLiteralAstNode;

public class BracketFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private AstNode startExpr;
	private AstNode endExpr = new NullLiteralAstNode();
	private boolean isRange;

	public BracketFieldAccessAstNode(AstNode src, @Nullable AstNode atExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteralAstNode();
		this.isRange = false;
	}

	public BracketFieldAccessAstNode(AstNode src, @Nullable AstNode startExpr, @Nullable AstNode endExpr, boolean permissive) {
		super(src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteralAstNode();
		this.endExpr = endExpr != null ? endExpr : new NullLiteralAstNode();
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
