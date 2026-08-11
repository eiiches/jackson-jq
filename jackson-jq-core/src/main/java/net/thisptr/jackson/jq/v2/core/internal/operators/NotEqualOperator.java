package net.thisptr.jackson.jq.v2.core.internal.operators;

public class NotEqualOperator<JsonNode> extends ComparisonOperator<JsonNode> {
	public NotEqualOperator() {
		super("!=");
	}

	@Override
	protected boolean test(int r) {
		return r != 0;
	}
}
