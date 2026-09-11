package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class BracketFieldAccessAstNode extends AbstractFieldAccessAstNode {
	private final AstNode startExpr;
	private final AstNode endExpr;
	private final boolean isRange;

	public BracketFieldAccessAstNode(SourceLocation location, AstNode src, @Nullable AstNode atExpr, boolean permissive) {
		super(location, src, permissive);
		this.startExpr = atExpr != null ? atExpr : new NullLiteralAstNode(location);
		this.endExpr = new NullLiteralAstNode(location);
		this.isRange = false;
	}

	public BracketFieldAccessAstNode(SourceLocation location, AstNode src, @Nullable AstNode startExpr, @Nullable AstNode endExpr, boolean permissive) {
		super(location, src, permissive);
		this.startExpr = startExpr != null ? startExpr : new NullLiteralAstNode(location);
		this.endExpr = endExpr != null ? endExpr : new NullLiteralAstNode(location);
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
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
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
