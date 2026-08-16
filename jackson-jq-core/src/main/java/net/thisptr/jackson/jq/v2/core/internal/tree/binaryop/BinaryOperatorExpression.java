package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.BinaryOperatorExpression.Operator.Associativity;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.Assignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexAlternativeAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexDivideAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMinusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexModuloAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexMultiplyAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.ComplexPlusAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment.UpdateAssignment;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareGreaterEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareGreaterTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareLessEqualTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareLessTest;
import net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison.CompareNotEqualTest;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;

public abstract class BinaryOperatorExpression implements Expression {
	protected final Expression lhs;
	protected final Expression rhs;
	private final String image;

	public BinaryOperatorExpression(Expression lhs, Expression rhs, String image) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.image = image;
	}

	public Expression lhs() {
		return lhs;
	}

	public Expression rhs() {
		return rhs;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)", lhs, image, rhs);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public enum Operator {
		ASSIGN("=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new Assignment(lhs, rhs);
			}
		},
		UDPATE("|=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new UpdateAssignment(lhs, rhs, version);
			}
		},
		DEFAULT_EQUAL("//=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexAlternativeAssignment(lhs, rhs);
			}
		},
		PLUS_EQUAL("+=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexPlusAssignment(lhs, rhs);
			}
		},
		MINUS_EQUAL("-=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexMinusAssignment(lhs, rhs);
			}
		},
		TIMES_EQUAL("*=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexMultiplyAssignment(lhs, rhs);
			}
		},
		DIVIDE_EQUAL("/=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexDivideAssignment(lhs, rhs);
			}
		},
		MODULO_EQUAL("%=", 6, Associativity.RIGHT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ComplexModuloAssignment(lhs, rhs);
			}
		},
		DEFAULT("//", 5, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new AlternativeOperatorExpression(lhs, rhs);
			}
		},
		OR("or", 4, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new BooleanOrExpression(lhs, rhs);
			}
		},
		AND("and", 4, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new BooleanAndExpression(lhs, rhs);
			}
		},
		LESS_EQUAL("<=", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareLessEqualTest(lhs, rhs);
			}
		},
		LESS("<", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareLessTest(lhs, rhs);
			}
		},
		GREATER_EQUAL(">=", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareGreaterEqualTest(lhs, rhs);
			}
		},
		GREATER(">", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareGreaterTest(lhs, rhs);
			}
		},
		EQUAL("==", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareEqualTest(lhs, rhs);
			}
		},
		NOT_EQUAL("!=", 3, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new CompareNotEqualTest(lhs, rhs);
			}
		},
		PLUS("+", 2, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new PlusExpression(lhs, rhs);
			}
		},
		MINUS("-", 2, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new MinusExpression(lhs, rhs);
			}
		},
		MODULO("%", 1, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new ModuloExpression(lhs, rhs);
			}
		},
		DIVIDE("/", 1, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
				return new DivideExpression(lhs, rhs);
			}
		},
		TIMES("*", 1, Associativity.LEFT) {
			@Override
			public Expression create(Expression lhs, Expression rhs, Version version) {
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
		public abstract Expression create(Expression lhs, Expression rhs, Version version);

		public enum Associativity {
			LEFT, RIGHT
		}

		Operator(String image, int precedence, Associativity associativity) {
			this.image = image;
			this.precedence = precedence;
			this.associativity = associativity;
		}

		public static Operator fromImage(String image) {
			Operator op = lookup.get(image);
			if (op == null)
				throw new IllegalArgumentException();
			return op;
		}

		private static final Map<String, Operator> lookup = new HashMap<>();
		static {
			for (Operator op : Operator.values())
				lookup.put(op.image, op);
		}
	}

	/**
	 * Raw types version for JavaCC compatibility.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static AstNode buildTree(List exprs, List<Operator> operators) {
		return buildTreeGeneric((List<AstNode>) exprs, operators);
	}

	public static AstNode buildTreeGeneric(List<AstNode> exprs, List<Operator> operators) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		Stack<AstNode> stackExprs = new Stack<>();
		Stack<Operator> stackOperators = new Stack<>();

		Iterator<AstNode> iterExpr = exprs.iterator();
		Iterator<Operator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			Operator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				Operator op2 = stackOperators.peek();
				if (op1.precedence > op2.precedence
						|| op1.precedence == op2.precedence && op1.associativity == Associativity.LEFT) {
					Operator op = stackOperators.pop();
					AstNode rhs = stackExprs.pop();
					AstNode lhs = stackExprs.pop();
					stackExprs.push(new BinaryOpAstNode(op, lhs, rhs));
				} else {
					break;
				}
			}
			stackOperators.push(op1);
			stackExprs.push(iterExpr.next());
		}

		while (!stackOperators.isEmpty()) {
			Operator op = stackOperators.pop();
			AstNode rhs = stackExprs.pop();
			AstNode lhs = stackExprs.pop();
			stackExprs.push(new BinaryOpAstNode(op, lhs, rhs));
		}

		return stackExprs.get(0);
	}
}
