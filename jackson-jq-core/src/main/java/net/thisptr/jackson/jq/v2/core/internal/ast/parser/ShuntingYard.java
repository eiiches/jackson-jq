package net.thisptr.jackson.jq.v2.core.internal.ast.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.operator.BinaryOperator;

public class ShuntingYard {
	private ShuntingYard() {
	}

	public static AstNode buildTree(List<AstNode> exprs, List<ParsedInfixOperator> operators) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		Deque<AstNode> stackExprs = new ArrayDeque<>();
		Deque<ParsedInfixOperator> stackOperators = new ArrayDeque<>();

		Iterator<AstNode> iterExpr = exprs.iterator();
		Iterator<ParsedInfixOperator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			ParsedInfixOperator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				ParsedInfixOperator op2 = stackOperators.peek();
				if (op1.precedence() > op2.precedence()
						|| (op1.precedence() == op2.precedence() && op1.associativity() == BinaryOperator.Associativity.LEFT)) {
					ParsedInfixOperator op = stackOperators.pop();
					AstNode rhs = stackExprs.pop();
					AstNode lhs = stackExprs.pop();
					stackExprs.push(op.createNode(lhs, rhs));
				} else {
					break;
				}
			}
			stackOperators.push(op1);
			stackExprs.push(iterExpr.next());
		}

		while (!stackOperators.isEmpty()) {
			ParsedInfixOperator op = stackOperators.pop();
			AstNode rhs = stackExprs.pop();
			AstNode lhs = stackExprs.pop();
			stackExprs.push(op.createNode(lhs, rhs));
		}

		AstNode result = stackExprs.pop();
		ParsedInfixOperator.checkCompleteExpression(result);
		return result;
	}
}
