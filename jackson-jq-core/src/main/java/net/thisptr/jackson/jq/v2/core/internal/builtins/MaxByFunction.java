package net.thisptr.jackson.jq.v2.core.internal.builtins;

import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "max_by", nargs = 1)
public class MaxByFunction extends AbstractMaxByFunction {
	public MaxByFunction() {
		super("max_by");
	}

	@Override
	protected <JsonNode> boolean isLarger(JsonProvider<JsonNode> jsonProvider, JsonNode criteria, JsonNode value) {
		JsonNodeComparator<JsonNode> comparator = new JsonNodeComparator<>(jsonProvider);
		int comparison = comparator.compare(criteria, value);
		// NaN keys compare less than each other in both directions. Keep the earlier item in that case.
		return comparison > 0 || (comparison < 0 && comparator.compare(value, criteria) < 0);
	}
}
