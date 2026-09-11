package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

public abstract class AbstractValueLiteralAstNode extends AbstractAstNode {

	protected AbstractValueLiteralAstNode(SourceLocation location) {
		super(location);
	}
}
