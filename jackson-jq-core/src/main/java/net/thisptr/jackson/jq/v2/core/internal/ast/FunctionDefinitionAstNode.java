package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

public class FunctionDefinitionAstNode extends AbstractAstNode {
	private final AstNode body;
	private final FunctionSignature signature;
	private final List<String> args;
	// The signatures a #jackson-jq:type comment stated for this definition, or empty for one that
	// stated none and is therefore read from its body.
	private final List<TypeScheme<FunctionType>> typeSchemes;

	public FunctionDefinitionAstNode(SourceLocation location, FunctionSignature signature, List<String> args, AstNode body,
									 List<TypeScheme<FunctionType>> typeSchemes) {
		super(location);
		this.signature = signature;
		this.args = args;
		this.body = body;
		this.typeSchemes = List.copyOf(typeSchemes);
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

	public List<TypeScheme<FunctionType>> typeSchemes() {
		return typeSchemes;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (TypeScheme<FunctionType> scheme : typeSchemes)
			builder.append(TypeAnnotation.MARKER).append(' ').append(scheme).append('\n');
		builder.append("def ");
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
