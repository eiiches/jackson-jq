package net.thisptr.jackson.jq.v2.core.internal.ast.impls;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class FunctionCallAstNode implements AstNode {
	private final String name;
	private final List<AstNode> args;
	private final @Nullable String moduleName;

	public FunctionCallAstNode(@Nullable String moduleName, String name, List<AstNode> args) {
		this.moduleName = moduleName;
		this.name = name;
		this.args = args;
	}

	public String name() {
		return name;
	}

	public List<AstNode> args() {
		return args;
	}

	public @Nullable String moduleName() {
		return moduleName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (moduleName != null) {
			builder.append(moduleName);
			builder.append("::");
		}
		builder.append(name);
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
