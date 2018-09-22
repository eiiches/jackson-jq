package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("min_by/1")
public class MinByFunction extends AbstractMaxByFunction {
	public MinByFunction() {
		super("min_by");
	}

	@Override
	protected boolean isLarger(final JsonNode criteria, final JsonNode value) {
		return comparator.compare(criteria, value) <= 0;
	}
}
