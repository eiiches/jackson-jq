package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class SemicolonOperatorAstNode extends AbstractAstNode {
	private final List<AstNode> qs;

	public SemicolonOperatorAstNode(SourceLocation location, List<AstNode> qs) {
		super(location);
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
