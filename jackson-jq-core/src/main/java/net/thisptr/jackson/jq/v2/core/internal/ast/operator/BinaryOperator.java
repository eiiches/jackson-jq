package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

import java.util.HashMap;
import java.util.Map;

public enum BinaryOperator {
	PIPE("|", 8, Associativity.RIGHT),
	BINDING_PIPE("|", 8, Associativity.RIGHT),
	COMMA(",", 7, Associativity.LEFT),
	ASSIGN("=", 6, Associativity.RIGHT),
	UPDATE("|=", 6, Associativity.RIGHT),
	DEFAULT_EQUAL("//=", 6, Associativity.RIGHT),
	PLUS_EQUAL("+=", 6, Associativity.RIGHT),
	MINUS_EQUAL("-=", 6, Associativity.RIGHT),
	TIMES_EQUAL("*=", 6, Associativity.RIGHT),
	DIVIDE_EQUAL("/=", 6, Associativity.RIGHT),
	MODULO_EQUAL("%=", 6, Associativity.RIGHT),
	DEFAULT("//", 5, Associativity.LEFT),
	OR("or", 4, Associativity.LEFT),
	AND("and", 4, Associativity.LEFT),
	LESS_EQUAL("<=", 3, Associativity.LEFT),
	LESS("<", 3, Associativity.LEFT),
	GREATER_EQUAL(">=", 3, Associativity.LEFT),
	GREATER(">", 3, Associativity.LEFT),
	EQUAL("==", 3, Associativity.LEFT),
	NOT_EQUAL("!=", 3, Associativity.LEFT),
	PLUS("+", 2, Associativity.LEFT),
	MINUS("-", 2, Associativity.LEFT),
	MODULO("%", 1, Associativity.LEFT),
	DIVIDE("/", 1, Associativity.LEFT),
	TIMES("*", 1, Associativity.LEFT);

	public final String image;
	public final int precedence;
	public final Associativity associativity;

	public enum Associativity {
		LEFT, RIGHT
	}

	BinaryOperator(String image, int precedence, Associativity associativity) {
		this.image = image;
		this.precedence = precedence;
		this.associativity = associativity;
	}

	private static final Map<String, BinaryOperator> lookup = new HashMap<>();

	static {
		for (BinaryOperator op : BinaryOperator.values()) {
			if (op == BINDING_PIPE)
				continue;
			lookup.put(op.image, op);
		}
	}

	public static BinaryOperator fromString(String image) {
		BinaryOperator op = lookup.get(image);
		if (op == null)
			throw new IllegalArgumentException("Unknown operator: " + image);
		return op;
	}

	@Override
	public String toString() {
		return image;
	}
}
