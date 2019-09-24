package net.thisptr.jackson.jq.internal.operators;

public class GreaterEqualOperator extends ComparisonOperator {
	public GreaterEqualOperator() {
		super(">=");
	}

	@Override
	protected boolean test(int r) {
		return r >= 0;
	}
}
