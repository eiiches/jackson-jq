package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

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

	public static class Question extends TryCatchAstNode {
		public Question(AstNode tryExpr) {
			super(tryExpr);
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
