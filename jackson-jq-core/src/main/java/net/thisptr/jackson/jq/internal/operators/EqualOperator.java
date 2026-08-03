package net.thisptr.jackson.jq.internal.operators;

public class EqualOperator extends ComparisonOperator {
	public EqualOperator() {
		super("==");
	}

	@Override
	protected boolean test(int r) {
		return r == 0;
	}
}
