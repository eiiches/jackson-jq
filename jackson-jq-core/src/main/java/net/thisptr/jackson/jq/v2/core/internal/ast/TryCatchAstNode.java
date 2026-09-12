package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class TryCatchAstNode extends AbstractAstNode {
	protected final AstNode tryExpr;
	protected final @Nullable AstNode catchExpr;

	public TryCatchAstNode(SourceLocation location, AstNode tryExpr, @Nullable AstNode catchExpr) {
		super(location);
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatchAstNode(SourceLocation location, AstNode tryExpr) {
		this(location, tryExpr, null);
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
		public Question(SourceLocation location, AstNode tryExpr) {
			super(location, tryExpr);
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
