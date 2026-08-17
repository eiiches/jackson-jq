package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction;

public class ObjectConstructionAstNode implements AstNode {
	public final List<FieldConstructionAst> fields = new ArrayList<>();

	public ObjectConstructionAstNode() {
	}

	public void add(FieldConstructionAst field) {
		fields.add(field);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		@Var String sep = "";
		for (FieldConstructionAst field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * AST-side counterpart of {@link FieldConstruction}.
	 */
	public interface FieldConstructionAst extends AstNode {
	}

	public static class IdentifierKeyFieldConstructionAst implements FieldConstructionAst {
		public final String key;
		public final @Nullable AstNode value;

		public IdentifierKeyFieldConstructionAst(String key, @Nullable AstNode value) {
			this.key = key;
			this.value = value;
		}

		public IdentifierKeyFieldConstructionAst(String key) {
			this(key, null);
		}

		@Override
		public String toString() {
			if (value == null) {
				return key;
			} else {
				return key + ": " + value.toString();
			}
		}
	}

	public static class StringKeyFieldConstructionAst implements FieldConstructionAst {
		public final AstNode key;
		public final @Nullable AstNode value;

		public StringKeyFieldConstructionAst(AstNode key, @Nullable AstNode value) {
			this.key = key;
			this.value = value;
		}

		public StringKeyFieldConstructionAst(AstNode key) {
			this(key, null);
		}

		@Override
		public String toString() {
			if (value == null) {
				return key.toString();
			} else {
				return key.toString() + ": " + value.toString();
			}
		}
	}

	public static class VariableKeyFieldConstruction implements FieldConstructionAst {
		private final String name;

		public VariableKeyFieldConstruction(String name) {
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

	public static class JsonQueryKeyFieldConstructionAst implements FieldConstructionAst {
		private final AstNode key;
		private final AstNode value;

		public JsonQueryKeyFieldConstructionAst(AstNode key, AstNode value) {
			this.key = key;
			this.value = value;
		}

		public AstNode key() {
			return key;
		}

		public AstNode value() {
			return value;
		}

		@Override
		public String toString() {
			String result = "(" + key.toString() + ")";
			return result + ": " + value;
		}
	}
}
