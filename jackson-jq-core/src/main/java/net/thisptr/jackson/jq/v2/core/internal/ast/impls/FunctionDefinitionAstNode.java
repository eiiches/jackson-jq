package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class FunctionDefinitionAstNode implements AstNode {
	private AstNode body;
	private String fname;
	private List<String> args;

	public FunctionDefinitionAstNode(String fname, List<String> args, AstNode body) {
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
