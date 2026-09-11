package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;


public class TryCatchAstNode implements AstNode {
	protected final AstNode tryExpr;
	protected final @Nullable AstNode catchExpr;

	public TryCatchAstNode(AstNode tryExpr, @Nullable AstNode catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatchAstNode(AstNode tryExpr) {
		this(tryExpr, null);
	}

	public AstNode tryExpr() {
		return tryExpr;
	}

	public @Nullable AstNode catchExpr() {
		return catchExpr;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	public static class Question extends TryCatchAstNode {
		public Question(AstNode tryExpr) {
			super(tryExpr);
		}

		@Override
		public <R> R accept(AstVisitor<R> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return tryExpr + "?";
		}
	}

	@Override
	public String toString() {
		if (catchExpr != null) {
			return String.format("try %s catch %s", tryExpr, catchExpr);
		} else {
			return String.format("try %s", tryExpr);
		}
	}
}
