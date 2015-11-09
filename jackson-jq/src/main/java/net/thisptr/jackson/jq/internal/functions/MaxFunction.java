package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;

@BuiltinFunction("max/1")
public class MaxFunction extends AbstractMaxFunction {
	public MaxFunction() {
		super("max");
	}

	@Override
	protected boolean isLarger(final JsonNode criteria, final JsonNode value) {
		return comparator.compare(criteria, value) > 0;
	}
}
