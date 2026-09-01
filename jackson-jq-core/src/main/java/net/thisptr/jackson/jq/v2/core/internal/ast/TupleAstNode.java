package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

public class TupleAstNode implements AstNode {
	public final List<AstNode> qs;

	public TupleAstNode(List<AstNode> qs) {
		this.qs = qs;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		@Var String separator = "";
		for (AstNode q : qs) {
			builder.append(separator);
			builder.append(q);
			separator = ", ";
		}
		return builder.toString();
	}
}
