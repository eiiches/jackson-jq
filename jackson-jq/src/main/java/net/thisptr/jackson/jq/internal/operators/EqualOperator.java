package net.thisptr.jackson.jq.internal.operators;

public class EqualOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public EqualOperator() {
		super("==");
	}

	@Override
	protected boolean test(int r) {
		return r == 0;
	}
}
