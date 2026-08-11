package net.thisptr.jackson.jq.v2.core.internal.operators;

public class LessOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public LessOperator() {
		super("<");
	}

	@Override
	protected boolean test(int r) {
		return r < 0;
	}
}
