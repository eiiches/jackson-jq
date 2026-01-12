package net.thisptr.jackson.jq.internal.operators;

public class GreaterEqualOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public GreaterEqualOperator() {
		super(">=");
	}

	@Override
	protected boolean test(int r) {
		return r >= 0;
	}
}
