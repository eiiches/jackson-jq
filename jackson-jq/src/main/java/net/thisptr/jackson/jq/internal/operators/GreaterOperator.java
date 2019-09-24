package net.thisptr.jackson.jq.internal.operators;

public class GreaterOperator extends ComparisonOperator {
	public GreaterOperator() {
		super(">");
	}

	@Override
	protected boolean test(int r) {
		return r > 0;
	}
}
