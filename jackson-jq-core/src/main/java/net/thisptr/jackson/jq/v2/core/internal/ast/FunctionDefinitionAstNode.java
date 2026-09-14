package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class FunctionDefinitionAstNode extends AbstractAstNode {
	private final AstNode body;
	private final String fname;
	private final List<String> args;

	public FunctionDefinitionAstNode(SourceLocation location, String fname, List<String> args, AstNode body) {
		super(location);
		this.fname = fname;
		this.args = args;
		this.body = body;
	}

	public String fname() {
		return fname;
	}

	public List<String> args() {
		return args;
	}

	public AstNode body() {
		return body;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("def ");
		builder.append(fname);
		if (!args.isEmpty()) {
			builder.append("(");
			@Var String sep = "";
			for (String arg : args) {
				builder.append(sep);
				builder.append(arg);
				sep = "; ";
			}
			builder.append(")");
		}
		builder.append(": ");
		builder.append(body);
		return builder.toString();
	}
}
