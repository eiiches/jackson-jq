package net.thisptr.jackson.jq.v2.core.internal.ast.binaryop;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression.Operator;

public class BinaryOpNode implements AstNode {
	public final Operator operator;
	public final AstNode lhs;
	public final AstNode rhs;

	public BinaryOpNode(Operator operator, AstNode lhs, AstNode rhs) {
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)", lhs, operator.image, rhs);
	}
}
