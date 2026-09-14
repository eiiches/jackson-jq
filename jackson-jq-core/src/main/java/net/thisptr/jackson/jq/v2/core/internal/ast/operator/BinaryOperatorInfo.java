package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

/**
 * Precedence and associativity of a binary operator for one jq language version.
 */
public final class BinaryOperatorInfo {
	private final int precedence;
	private final Associativity associativity;

	public enum Associativity {
		LEFT, RIGHT
	}

	public BinaryOperatorInfo(int precedence, Associativity associativity) {
		this.precedence = precedence;
		this.associativity = associativity;
	}

	public int getPrecedence() {
		return precedence;
	}

	public Associativity getAssociativity() {
		return associativity;
	}
}
