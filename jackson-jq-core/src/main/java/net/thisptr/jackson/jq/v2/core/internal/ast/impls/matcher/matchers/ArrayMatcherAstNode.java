package net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.matchers;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.impls.matcher.PatternMatcherAstNode;

public class ArrayMatcherAstNode implements PatternMatcherAstNode {
	private List<PatternMatcherAstNode> matchers;

	public ArrayMatcherAstNode(List<PatternMatcherAstNode> matchers) {
		this.matchers = matchers;
	}

	public List<PatternMatcherAstNode> matchers() {
		return matchers;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		@Var String sep = "";
		for (PatternMatcherAstNode matcher : matchers) {
			sb.append(sep);
			sb.append(matcher);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}
