package net.thisptr.jackson.jq.internal.tree.literal;

import com.fasterxml.jackson.databind.node.NullNode;

public class NullLiteral extends ValueLiteral {
	public NullLiteral() {
		super(NullNode.getInstance());
	}
}
