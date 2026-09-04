package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class RecursionOperatorAstNode implements AstNode {

	@Override
	public String toString() {
		return "(..)";
	}
}
