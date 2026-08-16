package net.thisptr.jackson.jq.v2.core.internal.ast.matcher.matchers;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.matcher.PatternMatcherAst;

public class ArrayMatcherAst implements PatternMatcherAst {
	private List<PatternMatcherAst> matchers;

	public ArrayMatcherAst(List<PatternMatcherAst> matchers) {
		this.matchers = matchers;
	}

	public List<PatternMatcherAst> matchers() {
		return matchers;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		@Var String sep = "";
		for (PatternMatcherAst matcher : matchers) {
			sb.append(sep);
			sb.append(matcher);
			sep = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}
