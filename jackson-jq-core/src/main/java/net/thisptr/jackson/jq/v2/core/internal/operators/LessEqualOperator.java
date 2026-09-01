package net.thisptr.jackson.jq.v2.core.internal.operators;

public class LessEqualOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public LessEqualOperator() {
		super("<=");
	}

	@Override
	protected boolean test(int r) {
		return r <= 0;
	}
}
