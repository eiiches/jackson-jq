package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

public class TryCatch implements AstNode {
	protected AstNode tryExpr;
	protected @Nullable AstNode catchExpr;

	public TryCatch(AstNode tryExpr, @Nullable AstNode catchExpr) {
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(AstNode tryExpr) {
		this(tryExpr, null);
	}

	public AstNode tryExpr() {
		return tryExpr;
	}

	public @Nullable AstNode catchExpr() {
		return catchExpr;
	}

	public static class Question extends TryCatch {
		public Question(AstNode tryExpr) {
			super(tryExpr);
		}

		@Override
		public String toString() {
			return String.format("(%s)?", tryExpr);
		}
	}

	@Override
	public String toString() {
		if (catchExpr != null) {
			return String.format("(try (%s) catch (%s))", tryExpr, catchExpr);
		} else {
			return String.format("(try (%s))", tryExpr);
		}
	}
}
