package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.node.TextNode;

public class StringLiteral extends ValueLiteral {
	public StringLiteral(final String text) {
		super(new TextNode(text));
	}
}
