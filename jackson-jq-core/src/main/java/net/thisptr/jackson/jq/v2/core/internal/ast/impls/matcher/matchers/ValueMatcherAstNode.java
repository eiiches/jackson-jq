package net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers;

import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.PatternMatcherAstNode;

public class ValueMatcherAstNode implements PatternMatcherAstNode {
	private final String name;

	public ValueMatcherAstNode(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
