package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

public class FunctionDefinitionAstNode extends AbstractAstNode {
	private final AstNode body;
	private final FunctionSignature signature;
	private final List<String> args;

	public FunctionDefinitionAstNode(SourceLocation location, FunctionSignature signature, List<String> args, AstNode body) {
		super(location);
		this.signature = signature;
		this.args = args;
		this.body = body;
	}

	public FunctionSignature signature() {
		return signature;
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
		builder.append(signature.name());
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
