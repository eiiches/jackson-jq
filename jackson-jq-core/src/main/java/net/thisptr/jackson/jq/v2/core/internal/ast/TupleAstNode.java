package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class TupleAstNode extends AbstractAstNode {
	public final List<AstNode> qs;

	public TupleAstNode(SourceLocation location, List<AstNode> qs) {
		super(location);
		this.qs = qs;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
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
