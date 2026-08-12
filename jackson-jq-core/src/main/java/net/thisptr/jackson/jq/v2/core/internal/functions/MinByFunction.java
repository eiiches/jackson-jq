package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("min_by/1")
public class MinByFunction extends AbstractMaxByFunction {
	public MinByFunction() {
		super("min_by");
	}

	@Override
	protected <JsonNode> boolean isLarger(final JsonProvider<JsonNode> jsonProvider, final JsonNode criteria, final JsonNode value) {
		return new JsonNodeComparator<>(jsonProvider).compare(criteria, value) <= 0;
	}
}
