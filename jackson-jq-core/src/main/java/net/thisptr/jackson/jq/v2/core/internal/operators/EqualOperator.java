package net.thisptr.jackson.jq.v2.core.internal.operators;

public class EqualOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public EqualOperator() {
		super("==");
	}

	@Override
	protected boolean test(int r) {
		return r == 0;
	}
}
