package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.node.DoubleNode;

public class DoubleLiteral extends ValueLiteral {
	public DoubleLiteral(final double value) {
		super(new DoubleNode(value));
	}
}
