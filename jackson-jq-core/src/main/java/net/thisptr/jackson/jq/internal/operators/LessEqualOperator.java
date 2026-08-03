package net.thisptr.jackson.jq.internal.operators;

public class LessEqualOperator extends ComparisonOperator {
	public LessEqualOperator() {
		super("<=");
	}

	@Override
	protected boolean test(int r) {
		return r <= 0;
	}
}
