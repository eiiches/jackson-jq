package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;

@BuiltinFunction("min/1")
public class MinFunction extends AbstractMaxFunction {
	public MinFunction() {
		super("min");
	}

	@Override
	protected boolean isLarger(final JsonNode criteria, final JsonNode value) {
		return comparator.compare(criteria, value) <= 0;
	}
}
