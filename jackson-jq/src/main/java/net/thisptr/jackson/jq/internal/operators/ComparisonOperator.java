package net.thisptr.jackson.jq.internal.operators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

public abstract class ComparisonOperator implements BinaryOperator {
	private static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();
	private String image;

	public ComparisonOperator(final String image) {
		this.image = image;
	}

	protected abstract boolean test(final int r);

	@Override
	public JsonNode apply(ObjectMapper mapper, JsonNode lhs, JsonNode rhs) throws JsonQueryException {
		final int r = comparator.compare(lhs, rhs);
		return BooleanNode.valueOf(test(r));
	}

	@Override
	public String image() {
		return image;
	}
}
