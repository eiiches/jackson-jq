package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

public class TupleAstNode implements AstNode {
	public final List<AstNode> qs;

	public TupleAstNode(List<AstNode> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		return qs.toString().replaceAll("^\\[", "(").replaceAll("\\]$", ")");
	}
}
