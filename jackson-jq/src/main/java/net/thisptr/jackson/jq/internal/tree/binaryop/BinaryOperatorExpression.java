package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression.Operator.Associativity;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.Assignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexAlternativeAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexDivideAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexMinusAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexModuloAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexMultiplyAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexPlusAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.UpdateAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareGreaterEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareGreaterTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareLessEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareLessTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareNotEqualTest;

public abstract class BinaryOperatorExpression implements Expression {
	protected Expression lhs;
	protected Expression rhs;
	private String image;

	public BinaryOperatorExpression(final Expression lhs, final Expression rhs, final String image) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.image = image;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)", lhs, image, rhs);
	}

	public enum Operator {
		ASSIGN("=", 6, Associativity.RIGHT, Assignment.class),
		UDPATE("|=", 6, Associativity.RIGHT, UpdateAssignment.class),
		DEFAULT_EQUAL("//=", 6, Associativity.RIGHT, ComplexAlternativeAssignment.class),
		PLUS_EQUAL("+=", 6, Associativity.RIGHT, ComplexPlusAssignment.class),
		MINUS_EQUAL("-=", 6, Associativity.RIGHT, ComplexMinusAssignment.class),
		TIMES_EQUAL("*=", 6, Associativity.RIGHT, ComplexMultiplyAssignment.class),
		DIVIDE_EQUAL("/=", 6, Associativity.RIGHT, ComplexDivideAssignment.class),
		MODULO_EQUAL("%=", 6, Associativity.RIGHT, ComplexModuloAssignment.class),
		DEFAULT("//", 5, Associativity.LEFT, AlternativeOperatorExpression.class),
		OR("or", 4, Associativity.LEFT, BooleanOrExpression.class),
		AND("and", 4, Associativity.LEFT, BooleanAndExpression.class),
		LESS_EQUAL("<=", 3, Associativity.LEFT, CompareLessEqualTest.class),
		LESS("<", 3, Associativity.LEFT, CompareLessTest.class),
		GREATER_EQUAL(">=", 3, Associativity.LEFT, CompareGreaterEqualTest.class),
		GREATER(">", 3, Associativity.LEFT, CompareGreaterTest.class),
		EQUAL("==", 3, Associativity.LEFT, CompareEqualTest.class),
		NOT_EQUAL("!=", 3, Associativity.LEFT, CompareNotEqualTest.class),
		PLUS("+", 2, Associativity.LEFT, PlusExpression.class),
		MINUS("-", 2, Associativity.LEFT, MinusExpression.class),
		MODULO("%", 1, Associativity.LEFT, ModuloExpression.class),
		DIVIDE("/", 1, Associativity.LEFT, DivideExpression.class),
		TIMES("*", 1, Associativity.LEFT, MultiplyExpression.class);

		public final String image;
		public final int precedence;
		public final Associativity associativity;
		public final Class<? extends BinaryOperatorExpression> clazz;
		public final Constructor<? extends BinaryOperatorExpression> constructor;

		public enum Associativity {
			LEFT, RIGHT
		}

		private Operator(final String image, final int precedence, final Associativity associativity, final Class<? extends BinaryOperatorExpression> clazz) {
			this.image = image;
			this.precedence = precedence;
			this.associativity = associativity;
			this.clazz = clazz;
			try {
				this.constructor = clazz.getConstructor(Expression.class, Expression.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public static Operator fromImage(final String image) {
			final Operator op = lookup.get(image);
			if (op == null)
				throw new IllegalArgumentException();
			return op;
		}

		private static Map<String, Operator> lookup = new HashMap<>();
		static {
			for (final Operator op : Operator.values())
				lookup.put(op.image, op);
		}

		public Expression buildTree(Expression lhs, Expression rhs) {
			try {
				return constructor.newInstance(lhs, rhs);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Expression buildTree(final List<Expression> exprs, final List<Operator> operators) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		final Stack<Expression> stackExprs = new Stack<>();
		final Stack<Operator> stackOperators = new Stack<>();

		final Iterator<Expression> iterExpr = exprs.iterator();
		final Iterator<Operator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			final Operator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				final Operator op2 = stackOperators.peek();
				if (op1.precedence > op2.precedence
						|| op1.precedence == op2.precedence && op1.associativity == Associativity.LEFT) {
					final Operator op = stackOperators.pop();
					final Expression rhs = stackExprs.pop();
					final Expression lhs = stackExprs.pop();
					stackExprs.push(op.buildTree(lhs, rhs));
				} else {
					break;
				}
			}
			stackOperators.push(op1);
			stackExprs.push(iterExpr.next());
		}

		while (!stackOperators.isEmpty()) {
			final Operator op = stackOperators.pop();
			final Expression rhs = stackExprs.pop();
			final Expression lhs = stackExprs.pop();
			stackExprs.push(op.buildTree(lhs, rhs));
		}

		return stackExprs.get(0);
	}
}
