package net.thisptr.jackson.jq.v2.core.internal.operators;

public class GreaterOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public GreaterOperator() {
		super(">");
	}

	@Override
	protected boolean test(int r) {
		return r > 0;
	}
}
