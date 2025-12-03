package net.thisptr.jackson.jq.internal.tree.literal;

import tools.jackson.databind.node.StringNode;

public class StringLiteral extends ValueLiteral {
	public StringLiteral(final String text) {
		super(new StringNode(text));
	}
}
