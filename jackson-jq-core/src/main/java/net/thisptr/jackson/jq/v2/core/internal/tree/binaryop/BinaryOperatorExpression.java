package net.thisptr.jackson.jq.v2.core.internal.tree.binaryop;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.BinaryOpAstNode;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
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
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Version;

public abstract class BinaryOperatorExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	protected final Expression<StackFrame, JsonNode> lhs;
	protected final Expression<StackFrame, JsonNode> rhs;
	private final String image;
	// Default `lhs || rhs` formulas shared by every non-assignment operator (arithmetic, comparison,
	// and/or, //). The assignment family (whose dependsOnInput additionally depends on whether `.`
	// itself is known fixed -- see Assignment/ComplexAssignment/UpdateAssignment) combines this with
	// its own flag via super.dependsOnInput() rather than overriding this field directly.
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public BinaryOperatorExpression(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, String image) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.image = image;
		this.dependsOnInput = lhs.dependsOnInput() || rhs.dependsOnInput();
		this.dependsOnExternalState = lhs.dependsOnExternalState() || rhs.dependsOnExternalState();
		this.freeLocalSlots = FreeVariables.union(lhs, rhs);
		this.hasOpaqueVariableReference = FreeVariables.anyOpaque(lhs, rhs);
	}

	public Expression<StackFrame, JsonNode> lhs() {
		return lhs;
	}

	public Expression<StackFrame, JsonNode> rhs() {
		return rhs;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)", lhs, image, rhs);
	}

	public enum Operator {
		ASSIGN("=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new Assignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		UDPATE("|=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new UpdateAssignment<>(jsonProvider, lhs, rhs, version, inputFixed);
			}
		},
		DEFAULT_EQUAL("//=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexAlternativeAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		PLUS_EQUAL("+=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexPlusAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		MINUS_EQUAL("-=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexMinusAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		TIMES_EQUAL("*=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexMultiplyAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		DIVIDE_EQUAL("/=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexDivideAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		MODULO_EQUAL("%=", 6, Associativity.RIGHT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return create(lhs, rhs, version, jsonProvider, false);
			}

			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
				return new ComplexModuloAssignment<>(jsonProvider, lhs, rhs, inputFixed);
			}
		},
		DEFAULT("//", 5, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new AlternativeOperatorExpression<>(jsonProvider, lhs, rhs);
			}
		},
		OR("or", 4, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new BooleanOrExpression<>(jsonProvider, lhs, rhs);
			}
		},
		AND("and", 4, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new BooleanAndExpression<>(jsonProvider, lhs, rhs);
			}
		},
		LESS_EQUAL("<=", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareLessEqualTest<>(jsonProvider, lhs, rhs);
			}
		},
		LESS("<", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareLessTest<>(jsonProvider, lhs, rhs);
			}
		},
		GREATER_EQUAL(">=", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareGreaterEqualTest<>(jsonProvider, lhs, rhs);
			}
		},
		GREATER(">", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareGreaterTest<>(jsonProvider, lhs, rhs);
			}
		},
		EQUAL("==", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareEqualTest<>(jsonProvider, lhs, rhs);
			}
		},
		NOT_EQUAL("!=", 3, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new CompareNotEqualTest<>(jsonProvider, lhs, rhs);
			}
		},
		PLUS("+", 2, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new PlusExpression<>(jsonProvider, lhs, rhs, version);
			}
		},
		MINUS("-", 2, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new MinusExpression<>(jsonProvider, lhs, rhs, version);
			}
		},
		MODULO("%", 1, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new ModuloExpression<>(jsonProvider, lhs, rhs, version);
			}
		},
		DIVIDE("/", 1, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new DivideExpression<>(jsonProvider, lhs, rhs, version);
			}
		},
		TIMES("*", 1, Associativity.LEFT) {
			@Override
			public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider) {
				return new MultiplyExpression<>(jsonProvider, lhs, rhs, version);
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
		public abstract <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider);

		/**
		 * As {@link #create(Expression, Expression, Version, JsonProvider)}, but additionally told
		 * whether the {@code .} this operator is being compiled against is itself known to be fixed --
		 * only the assignment-family operators (which fall back to the raw, unmodified input when their
		 * lhs path expression matches nothing) need this; every other operator ignores it.
		 */
		public <JsonNode> Expression<StackFrame, JsonNode> create(Expression<StackFrame, JsonNode> lhs, Expression<StackFrame, JsonNode> rhs, Version version, JsonProvider<JsonNode> jsonProvider, boolean inputFixed) {
			return create(lhs, rhs, version, jsonProvider);
		}

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

	public static AstNode buildTree(List<AstNode> exprs, List<Operator> operators) {
		return buildTreeGeneric(exprs, operators);
	}

	public static AstNode buildTreeGeneric(List<AstNode> exprs, List<Operator> operators) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		Deque<AstNode> stackExprs = new ArrayDeque<>();
		Deque<Operator> stackOperators = new ArrayDeque<>();

		Iterator<AstNode> iterExpr = exprs.iterator();
		Iterator<Operator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			Operator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				Operator op2 = stackOperators.peek();
				if (op1.precedence > op2.precedence
						|| (op1.precedence == op2.precedence && op1.associativity == Operator.Associativity.LEFT)) {
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

		return stackExprs.pop();
	}
}
