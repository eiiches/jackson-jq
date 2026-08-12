package net.thisptr.jackson.jq.v2.core.internal.operators;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public abstract class ComparisonOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private String image;

	public ComparisonOperator(String image) {
		this.image = image;
	}

	protected abstract boolean test(int r);

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		int r = new JsonNodeComparator<>(jsonProvider).compare(lhs, rhs);
		return jsonProvider.createBoolean(test(r));
	}

	@Override
	public String image() {
		return image;
	}
}
