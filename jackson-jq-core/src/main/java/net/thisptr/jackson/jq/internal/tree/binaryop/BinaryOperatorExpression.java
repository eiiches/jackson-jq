package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Version;
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
		ASSIGN("=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new Assignment(lhs, rhs);
			}
		},
		UDPATE("|=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new UpdateAssignment(lhs, rhs, version);
			}
		},
		DEFAULT_EQUAL("//=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexAlternativeAssignment(lhs, rhs);
			}
		},
		PLUS_EQUAL("+=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexPlusAssignment(lhs, rhs);
			}
		},
		MINUS_EQUAL("-=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexMinusAssignment(lhs, rhs);
			}
		},
		TIMES_EQUAL("*=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexMultiplyAssignment(lhs, rhs);
			}
		},
		DIVIDE_EQUAL("/=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexDivideAssignment(lhs, rhs);
			}
		},
		MODULO_EQUAL("%=", 6, Associativity.RIGHT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexModuloAssignment(lhs, rhs);
			}
		},
		DEFAULT("//", 5, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new AlternativeOperatorExpression(lhs, rhs);
			}
		},
		OR("or", 4, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new BooleanOrExpression(lhs, rhs);
			}
		},
		AND("and", 4, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new BooleanAndExpression(lhs, rhs);
			}
		},
		LESS_EQUAL("<=", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareLessEqualTest(lhs, rhs);
			}
		},
		LESS("<", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareLessTest(lhs, rhs);
			}
		},
		GREATER_EQUAL(">=", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareGreaterEqualTest(lhs, rhs);
			}
		},
		GREATER(">", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareGreaterTest(lhs, rhs);
			}
		},
		EQUAL("==", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareEqualTest(lhs, rhs);
			}
		},
		NOT_EQUAL("!=", 3, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareNotEqualTest(lhs, rhs);
			}
		},
		PLUS("+", 2, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new PlusExpression(lhs, rhs);
			}
		},
		MINUS("-", 2, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new MinusExpression(lhs, rhs);
			}
		},
		MODULO("%", 1, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new ModuloExpression(lhs, rhs);
			}
		},
		DIVIDE("/", 1, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new DivideExpression(lhs, rhs);
			}
		},
		TIMES("*", 1, Associativity.LEFT) {
			@Override
			protected Expression create(Expression lhs, Expression rhs, Version version) {
				return new MultiplyExpression(lhs, rhs);
			}
		};

		public final String image;
		public final int precedence;
		public final Associativity associativity;

		/**
		 * Creates a new {@link Expression} instance based on the provided left-hand side (lhs) expression,
		 * right-hand side (rhs) expression, and the specified version.
		 *
		 * @param lhs the left-hand side expression
		 * @param rhs the right-hand side expression
		 * @param version the version providing contextual information for the expression creation
		 * @return a new instance of {@link Expression} that represents the operation between the lhs and rhs expressions
		 */
		protected abstract Expression create(Expression lhs, Expression rhs, Version version);

		public enum Associativity {
			LEFT, RIGHT
		}

		private Operator(final String image, final int precedence, final Associativity associativity) {
			this.image = image;
			this.precedence = precedence;
			this.associativity = associativity;
		}

		public static Operator fromImage(final String image) {
			final Operator op = lookup.get(image);
			if (op == null)
				throw new IllegalArgumentException();
			return op;
		}

		private static final Map<String, Operator> lookup = new HashMap<>();
		static {
			for (final Operator op : Operator.values())
				lookup.put(op.image, op);
		}

		public Expression buildTree(final Expression lhs, final Expression rhs, final Version version) {
			try {
				return create(lhs, rhs, version);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Expression buildTree(final List<Expression> exprs, final List<Operator> operators, final Version version) {
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
					stackExprs.push(op.buildTree(lhs, rhs, version));
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
			stackExprs.push(op.buildTree(lhs, rhs, version));
		}

		return stackExprs.get(0);
	}
}
