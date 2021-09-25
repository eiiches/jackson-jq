package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("max_by/1")
public class MaxByFunction extends AbstractMaxByFunction {
	public MaxByFunction() {
		super("max_by");
	}

	@Override
	protected boolean isLarger(final JsonNode criteria, final JsonNode value) {
		return comparator.compare(criteria, value) > 0;
	}
}
