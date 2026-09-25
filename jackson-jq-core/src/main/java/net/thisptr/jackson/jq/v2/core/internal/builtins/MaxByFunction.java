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
		return new JsonNodeComparator<>(jsonProvider).compare(criteria, value) > 0;
	}
}
