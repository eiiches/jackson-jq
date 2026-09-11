package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;


public class SemicolonOperatorAstNode implements AstNode {
	private final List<AstNode> qs;

	public SemicolonOperatorAstNode(List<AstNode> qs) {
		this.qs = qs;
	}

	public List<AstNode> expressions() {
		return qs;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
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
