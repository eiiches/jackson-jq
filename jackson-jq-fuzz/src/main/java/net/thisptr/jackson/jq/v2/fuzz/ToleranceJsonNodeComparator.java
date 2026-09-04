package net.thisptr.jackson.jq.v2.fuzz;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.internal.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

/**
 * A {@link JsonNodeComparator} that treats two numbers as equal if they're within a small relative or
 * absolute tolerance of each other, to absorb floating-point precision differences between real jq's C
 * math library and jackson-jq's use of {@link Math} (e.g. in {@code log}, {@code pow}, {@code sqrt}),
 * without masking genuinely different values.
 */
public class ToleranceJsonNodeComparator extends JsonNodeComparator<JsonNode> {
	private static final long serialVersionUID = 1L;

	private static final double ABSOLUTE_EPSILON = 1e-9;
	private static final double RELATIVE_EPSILON = 1e-9;

	public ToleranceJsonNodeComparator() {
		super(Jackson2JsonProviderImpl.getInstance());
	}

	@Override
	protected int compareNumberNode(JsonNode o1, JsonNode o2) {
		double a = o1.doubleValue();
		double b = o2.doubleValue();
		double diff = Math.abs(a - b);
		if (diff <= ABSOLUTE_EPSILON || diff <= RELATIVE_EPSILON * Math.max(Math.abs(a), Math.abs(b)))
			return 0;
		return super.compareNumberNode(o1, o2);
	}
}
