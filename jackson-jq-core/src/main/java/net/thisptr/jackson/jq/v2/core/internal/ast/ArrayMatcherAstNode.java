package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class ArrayMatcherAstNode extends AbstractAstNode implements PatternMatcherAstNode {
	private final List<PatternMatcherAstNode> matchers;

	public ArrayMatcherAstNode(SourceLocation location, List<PatternMatcherAstNode> matchers) {
		super(location);
		this.matchers = matchers;
	}

	public List<PatternMatcherAstNode> matchers() {
		return matchers;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
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
