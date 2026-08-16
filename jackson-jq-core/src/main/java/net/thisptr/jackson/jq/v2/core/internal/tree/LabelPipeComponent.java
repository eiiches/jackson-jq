package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.PipeComponentAst;

public class LabelPipeComponent<JsonNode> implements PipeComponent<JsonNode>, PipeComponentAst, AstNode {
	public final String name;

	public LabelPipeComponent(String name) {
		this.name = name;
	}

	@Override
	public boolean canTerminatePipe() {
		return false;
	}

	@Override
	public String toString() {
		return "label $" + name;
	}
}
