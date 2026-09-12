package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

import java.util.HashMap;
import java.util.Map;

public enum BinaryOperator {
	PIPE("|"),
	BINDING_PIPE("|"),
	COMMA(","),
	ASSIGN("="),
	UPDATE("|="),
	DEFAULT_EQUAL("//="),
	PLUS_EQUAL("+="),
	MINUS_EQUAL("-="),
	TIMES_EQUAL("*="),
	DIVIDE_EQUAL("/="),
	MODULO_EQUAL("%="),
	DEFAULT("//"),
	OR("or"),
	AND("and"),
	LESS_EQUAL("<="),
	LESS("<"),
	GREATER_EQUAL(">="),
	GREATER(">"),
	EQUAL("=="),
	NOT_EQUAL("!="),
	PLUS("+"),
	MINUS("-"),
	MODULO("%"),
	DIVIDE("/"),
	TIMES("*");

	public final String image;

	BinaryOperator(String image) {
		this.image = image;
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
