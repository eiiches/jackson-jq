package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.commons.pair.Pair;

public class ConditionalAstNode implements AstNode {
	private final AstNode otherwise;
	private final List<Pair<AstNode, AstNode>> switches;

	public ConditionalAstNode(List<Pair<AstNode, AstNode>> switches, AstNode otherwise) {
		this.switches = switches;
		this.otherwise = otherwise;
	}

	public List<Pair<AstNode, AstNode>> switches() {
		return switches;
	}

	public AstNode otherwise() {
		return otherwise;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		@Var String ifstr = "if";
		StringBuilder builder = new StringBuilder();
		for (Pair<AstNode, AstNode> sw : switches) {
			builder.append(ifstr);
			builder.append(" ");
			builder.append(sw._1 != null ? sw._1 : "null");
			builder.append(" ");
			builder.append("then");
			builder.append(" ");
			builder.append(sw._2 != null ? sw._2 : "null");
			builder.append(" ");
			ifstr = "elif";
		}
		builder.append("else ");
		builder.append(otherwise != null ? otherwise : "null");
		builder.append(" ");
		builder.append("end");
		return builder.toString();
	}
}
