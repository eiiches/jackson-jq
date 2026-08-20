package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class TopLevelAstNode<JsonNode> implements AstNode {
	private final List<ImportStatement<JsonNode>> imports;
	private final AstNode expr;
	private final ModuleDirective<JsonNode> moduleDirective;

	public TopLevelAstNode(ModuleDirective<JsonNode> moduleDirective, List<ImportStatement<JsonNode>> imports, AstNode expr) {
		this.moduleDirective = moduleDirective;
		this.imports = imports;
		this.expr = expr;
	}

	public ModuleDirective<JsonNode> moduleDirective() {
		return moduleDirective;
	}

	public List<ImportStatement<JsonNode>> imports() {
		return imports;
	}

	public AstNode expr() {
		return expr;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (moduleDirective != null) {
			s.append(moduleDirective);
			s.append("; ");
		}
		for (ImportStatement<JsonNode> imp : imports) {
			s.append(imp);
			s.append("; ");
		}
		s.append(expr);
		return s.toString();
	}

	public static class ImportStatement<JsonNode> {
		public final String path;
		public final boolean dollarImport;
		public final String name;
		private final @Nullable AstNode metadataExpr;
		private @Nullable JsonNode metadata;
		private boolean metadataEvaluated = false;

		public ImportStatement(String path, boolean dollarImport, String name, @Nullable AstNode metadataExpr) {
			this.path = path;
			this.dollarImport = dollarImport;
			this.name = name;
			this.metadataExpr = metadataExpr;
		}

		public @Nullable AstNode metadataExpr() {
			return metadataExpr;
		}

		public @Nullable JsonNode getMetadata(JsonProvider<JsonNode> jsonProvider) {
			if (!metadataEvaluated) {
				if (metadataExpr != null) {
					this.metadata = ExpressionUtils.evaluateLiteralExpression(jsonProvider, metadataExpr);
					if (metadata == null)
						throw new IllegalArgumentException("Module metadata must be constant");
					if (jsonProvider.getNodeType(metadata) != JsonNodeType.OBJECT)
						throw new IllegalArgumentException("Module metadata must be an object");
				} else {
					this.metadata = null;
				}
				metadataEvaluated = true;
			}
			return metadata;
		}

		public String toString(JsonProvider<JsonNode> jsonProvider) {
			StringBuilder s = new StringBuilder();
			s.append("import ");
			s.append(jsonProvider.createString(path).toString());
			s.append(" as ");
			if (dollarImport)
				s.append('$');
			s.append(name);
			JsonNode md = getMetadata(jsonProvider);
			if (md != null) {
				s.append(' ');
				s.append(md);
			}
			return s.toString();
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("import \"");
			s.append(path);
			s.append("\" as ");
			if (dollarImport)
				s.append('$');
			s.append(name);
			// Can't show metadata without JsonProvider
			return s.toString();
		}
	}

	public static class ModuleDirective<JsonNode> {
		private final AstNode metadataExpr;
		private @Nullable JsonNode metadata;
		private boolean metadataEvaluated = false;

		public ModuleDirective(AstNode metadataExpr) {
			this.metadataExpr = metadataExpr;
		}

		public AstNode metadataExpr() {
			return metadataExpr;
		}

		public JsonNode getMetadata(JsonProvider<JsonNode> jsonProvider) {
			if (!metadataEvaluated) {
				this.metadata = ExpressionUtils.evaluateLiteralExpression(jsonProvider, metadataExpr);
				if (metadata == null)
					throw new IllegalArgumentException("Module metadata must be constant");
				if (jsonProvider.getNodeType(metadata) != JsonNodeType.OBJECT)
					throw new IllegalArgumentException("Module metadata must be an object");
				metadataEvaluated = true;
			}
			return Objects.requireNonNull(metadata);
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("module {...}");
			return s.toString();
		}
	}
}
