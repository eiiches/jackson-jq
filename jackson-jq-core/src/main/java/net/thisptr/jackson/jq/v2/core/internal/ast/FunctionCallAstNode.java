package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;


public class FunctionCallAstNode extends AbstractAstNode {
	private final FunctionSignature signature;
	private final List<AstNode> args;
	private final @Nullable String moduleName;

	public FunctionCallAstNode(SourceLocation location, @Nullable String moduleName, FunctionSignature signature, List<AstNode> args) {
		super(location);
		this.moduleName = moduleName;
		this.signature = signature;
		this.args = args;
	}

	public FunctionSignature signature() {
		return signature;
	}

	public List<AstNode> args() {
		return args;
	}

	public @Nullable String moduleName() {
		return moduleName;
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (moduleName != null) {
			builder.append(moduleName);
			builder.append("::");
		}
		builder.append(signature.name());
		if (!args.isEmpty()) {
			builder.append("(");
			@Var String sep = "";
			for (AstNode arg : args) {
				builder.append(sep);
				if (arg == null) {
					builder.append("null");
				} else {
					builder.append(arg.toString());
				}
				sep = "; ";
			}
			builder.append(")");
		}
		return builder.toString();
	}
}
