package net.thisptr.jackson.jq.internal.operators;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public abstract class ComparisonOperator<JsonNode> implements BinaryOperator<JsonNode> {
	private String image;

	public ComparisonOperator(final String image) {
		this.image = image;
	}

	protected abstract boolean test(final int r);

	@Override
	public JsonNode apply(JsonProvider<JsonNode> jsonProvider, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final int r = new JsonNodeComparator<>(jsonProvider).compare(lhs, rhs);
		return jsonProvider.createBoolean(test(r));
	}

	@Override
	public String image() {
		return image;
	}
}
