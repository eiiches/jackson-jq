package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

public class BinaryOpAstNode extends AbstractAstNode {
	public final BinaryOperator operator;
	public final AstNode lhs;
	public final AstNode rhs;

	public BinaryOpAstNode(SourceLocation location, BinaryOperator operator, AstNode lhs, AstNode rhs) {
		super(location);
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
		if (operator == BinaryOperator.COMMA)
			return String.format("%s, %s", lhs, rhs);
		return String.format("%s %s %s", lhs, operator, rhs);
	}
}

