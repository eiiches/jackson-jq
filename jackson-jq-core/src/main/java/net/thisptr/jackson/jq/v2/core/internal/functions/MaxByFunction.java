package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
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
