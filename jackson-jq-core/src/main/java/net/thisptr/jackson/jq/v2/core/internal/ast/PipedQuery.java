package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

public class PipedQuery implements AstNode {
	private List<PipeComponentAst> components;

	public PipedQuery(List<PipeComponentAst> components) {
		this.components = components;
	}

	public List<PipeComponentAst> components() {
		return components;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("(");
		@Var String sep = "";
		for (PipeComponentAst component : components) {
			builder.append(sep);
			builder.append(component.toString());
			sep = " | ";
		}
		builder.append(")");
		return builder.toString();
	}
}
