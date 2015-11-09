package net.thisptr.jackson.jq.internal.operators;

public class LessOperator extends ComparisonOperator {
	public LessOperator() {
		super("<");
	}

	@Override
	protected boolean test(int r) {
		return r < 0;
	}
}
