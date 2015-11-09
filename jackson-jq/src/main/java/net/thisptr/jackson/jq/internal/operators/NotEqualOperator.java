package net.thisptr.jackson.jq.internal.operators;

public class NotEqualOperator extends ComparisonOperator {
	public NotEqualOperator() {
		super("!=");
	}

	@Override
	protected boolean test(int r) {
		return r != 0;
	}
}
