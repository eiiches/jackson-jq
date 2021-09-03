package net.thisptr.jackson.jq.internal.tree.literal;

import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

public class LongLiteral extends ValueLiteral {

	public LongLiteral() {}

	public LongLiteral(final long value) {
		super(JsonNodeUtils.asNumericNode(value));
	}
}
