package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.node.BooleanNode;

public class BooleanLiteral extends ValueLiteral {
	public BooleanLiteral(final boolean value) {
		super(BooleanNode.valueOf(value));
	}
}
