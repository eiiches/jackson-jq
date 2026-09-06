package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class SemicolonOperatorAstNode implements AstNode {
	private List<AstNode> qs;

	public SemicolonOperatorAstNode(List<AstNode> qs) {
		this.qs = qs;
	}

	public List<AstNode> expressions() {
		return qs;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		@Var String sep = "";
		for (AstNode q : qs) {
			builder.append(sep);
			builder.append(q);
			sep = "; ";
		}
		return builder.toString();
	}
}
