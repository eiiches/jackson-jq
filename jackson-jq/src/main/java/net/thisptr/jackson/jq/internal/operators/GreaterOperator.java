package net.thisptr.jackson.jq.internal.operators;

public class GreaterOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public GreaterOperator() {
		super(">");
	}

	@Override
	protected boolean test(int r) {
		return r > 0;
	}
}
