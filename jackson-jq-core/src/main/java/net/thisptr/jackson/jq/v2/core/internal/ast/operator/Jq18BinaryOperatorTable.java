package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

import java.util.EnumMap;

public final class Jq18BinaryOperatorTable implements BinaryOperatorTable {
	public static final Jq18BinaryOperatorTable INSTANCE = new Jq18BinaryOperatorTable();

	private final EnumMap<BinaryOperator, BinaryOperatorInfo> operatorInfo = new EnumMap<>(BinaryOperator.class);

	private Jq18BinaryOperatorTable() {
		put(BinaryOperator.PIPE, 8, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.BINDING_PIPE, 7, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.COMMA, 7, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.ASSIGN, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.UPDATE, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.DEFAULT_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.PLUS_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.MINUS_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.TIMES_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.DIVIDE_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.MODULO_EQUAL, 6, BinaryOperatorInfo.Associativity.RIGHT);
		put(BinaryOperator.DEFAULT, 5, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.OR, 4, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.AND, 4, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.LESS_EQUAL, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.LESS, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.GREATER_EQUAL, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.GREATER, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.EQUAL, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.NOT_EQUAL, 3, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.PLUS, 2, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.MINUS, 2, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.MODULO, 1, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.DIVIDE, 1, BinaryOperatorInfo.Associativity.LEFT);
		put(BinaryOperator.TIMES, 1, BinaryOperatorInfo.Associativity.LEFT);
	}

	@Override
	public BinaryOperatorInfo getOperatorInfo(BinaryOperator operator) {
		BinaryOperatorInfo result = operatorInfo.get(operator);
		if (result == null)
			throw new IllegalArgumentException("Unknown binary operator: " + operator);
		return result;
	}

	private void put(BinaryOperator operator, int precedence, BinaryOperatorInfo.Associativity associativity) {
		operatorInfo.put(operator, new BinaryOperatorInfo(precedence, associativity));
	}
}
