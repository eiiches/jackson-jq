package net.thisptr.jackson.jq.v2.core.internal.ast.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

public class ShuntingYard {
	private ShuntingYard() {
	}

	public static AstNode buildTree(List<AstNode> exprs, List<BinaryOperator> operators) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		Deque<AstNode> stackExprs = new ArrayDeque<>();
		Deque<BinaryOperator> stackOperators = new ArrayDeque<>();

		Iterator<AstNode> iterExpr = exprs.iterator();
		Iterator<BinaryOperator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			BinaryOperator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				BinaryOperator op2 = stackOperators.peek();
				if (op1.precedence > op2.precedence
						|| (op1.precedence == op2.precedence && op1.associativity == BinaryOperator.Associativity.LEFT)) {
					BinaryOperator op = stackOperators.pop();
					AstNode rhs = stackExprs.pop();
					AstNode lhs = stackExprs.pop();
					stackExprs.push(new BinaryOpAstNode(SourceLocation.span(lhs.location(), rhs.location()), op, lhs, rhs));
				} else {
					break;
				}
			}
			stackOperators.push(op1);
			stackExprs.push(iterExpr.next());
		}

		while (!stackOperators.isEmpty()) {
			BinaryOperator op = stackOperators.pop();
			AstNode rhs = stackExprs.pop();
			AstNode lhs = stackExprs.pop();
			stackExprs.push(new BinaryOpAstNode(SourceLocation.span(lhs.location(), rhs.location()), op, lhs, rhs));
		}

		return stackExprs.pop();
	}
}
