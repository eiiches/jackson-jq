package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

@AutoService(Function.class)
@BuiltinFunction("min_by/1")
public class MinByFunction<JsonNode> extends AbstractMaxByFunction<JsonNode> {
	public MinByFunction() {
		super("min_by");
	}

	@Override
	protected boolean isLarger(final JsonProvider<JsonNode> jsonProvider, final JsonNode criteria, final JsonNode value) {
		return new JsonNodeComparator<>(jsonProvider).compare(criteria, value) <= 0;
	}
}
