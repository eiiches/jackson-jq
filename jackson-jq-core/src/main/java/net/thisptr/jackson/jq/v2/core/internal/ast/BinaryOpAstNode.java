package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

public class BinaryOpAstNode implements AstNode {
	public final BinaryOperator operator;
	public final AstNode lhs;
	public final AstNode rhs;

	public BinaryOpAstNode(BinaryOperator operator, AstNode lhs, AstNode rhs) {
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return String.format("%s %s %s", lhs, operator, rhs);
	}
}


