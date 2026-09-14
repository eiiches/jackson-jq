package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;


public class ObjectConstructionAstNode extends AbstractAstNode {
	public final List<FieldConstructionAst> fields;

	public ObjectConstructionAstNode(SourceLocation location, List<FieldConstructionAst> fields) {
		super(location);
		this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
	}

	@Override
	public <R> R accept(AstVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		@Var String sep = "";
		for (FieldConstructionAst field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ", ";
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * AST-side counterpart of {@link net.thisptr.jackson.jq.v2.core.internal.tree.FieldConstruction}.
	 */
	public interface FieldConstructionAst extends AstNode {
	}

	public static class IdentifierKeyFieldConstructionAst extends AbstractAstNode implements FieldConstructionAst {
		public final String key;
		public final @Nullable AstNode value;

		public IdentifierKeyFieldConstructionAst(SourceLocation location, String key, @Nullable AstNode value) {
			super(location);
			this.key = key;
			this.value = value;
		}

		public IdentifierKeyFieldConstructionAst(SourceLocation location, String key) {
			this(location, key, null);
		}

		@Override
		public <R> R accept(AstVisitor<R> visitor) {
			return visitor.visit(this);
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

	public static class StringKeyFieldConstructionAst extends AbstractAstNode implements FieldConstructionAst {
		public final AstNode key;
		public final @Nullable AstNode value;

		public StringKeyFieldConstructionAst(SourceLocation location, AstNode key, @Nullable AstNode value) {
			super(location);
			this.key = key;
			this.value = value;
		}

		public StringKeyFieldConstructionAst(SourceLocation location, AstNode key) {
			this(location, key, null);
		}

		@Override
		public <R> R accept(AstVisitor<R> visitor) {
			return visitor.visit(this);
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

	public static class VariableKeyFieldConstruction extends AbstractAstNode implements FieldConstructionAst {
		private final String name;

		public VariableKeyFieldConstruction(SourceLocation location, String name) {
			super(location);
			this.name = name;
		}

		public String name() {
			return name;
		}

		@Override
		public <R> R accept(AstVisitor<R> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "$" + name;
		}
	}

	public static class JsonQueryKeyFieldConstructionAst extends AbstractAstNode implements FieldConstructionAst {
		private final AstNode key;
		private final AstNode value;

		public JsonQueryKeyFieldConstructionAst(SourceLocation location, AstNode key, AstNode value) {
			super(location);
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
		public <R> R accept(AstVisitor<R> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			String result = "(" + key.toString() + ")";
			return result + ": " + value;
		}
	}
}
