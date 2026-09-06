package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.impls.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

public class SimpleModuleMeta implements ModuleMeta {
	private final @Nullable AstNode metadataExpr;
	private final List<Dependency> dependencies;

	public SimpleModuleMeta() {
		this(null, Collections.emptyList());
	}

	public SimpleModuleMeta(@Nullable AstNode metadataExpr, List<Dependency> dependencies) {
		this.metadataExpr = metadataExpr;
		this.dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies));
	}

	public static SimpleModuleMeta fromAst(@Nullable AstNode ast) {
		@Var AstNode metadataExpr = null;
		List<Dependency> dependencies = new ArrayList<>();
		if (ast instanceof TopLevelAstNode) {
			TopLevelAstNode top = (TopLevelAstNode) ast;
			if (top.moduleDirective() != null)
				metadataExpr = top.moduleDirective().metadataExpr();
			for (TopLevelAstNode.ImportStatement imp : top.imports())
				dependencies.add(new SimpleDependency(imp.path, imp.dollarImport, imp.name, imp.metadataExpr()));
		}
		return new SimpleModuleMeta(metadataExpr, dependencies);
	}

	@Override
	public <JsonNode> Map<String, JsonNode> getMetadata(JsonProvider<JsonNode> jsonProvider) {
		return evaluateMetadata(jsonProvider, metadataExpr);
	}

	@Override
	public List<Dependency> getDependencies() {
		return dependencies;
	}

	static <JsonNode> Map<String, JsonNode> evaluateMetadata(JsonProvider<JsonNode> jsonProvider, @Nullable AstNode expr) {
		if (expr == null)
			return Collections.emptyMap();
		JsonNode node = ExpressionUtils.evaluateLiteralExpression(jsonProvider, expr);
		if (node == null)
			throw new IllegalArgumentException("Module metadata must be constant");
		if (!jsonProvider.isObject(node))
			throw new IllegalArgumentException("Module metadata must be an object");
		Map<String, JsonNode> result = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> fields = jsonProvider.getObjectMembers(node);
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(result);
	}

	public static class SimpleDependency implements Dependency {
		private final String relpath;
		private final boolean isData;
		private final @Nullable String alias;
		private final @Nullable AstNode importMetadataExpr;

		public SimpleDependency(String relpath, boolean isData, @Nullable String alias, @Nullable AstNode importMetadataExpr) {
			this.relpath = Objects.requireNonNull(relpath);
			this.isData = isData;
			this.alias = alias;
			this.importMetadataExpr = importMetadataExpr;
		}

		@Override
		public String getRelpath() {
			return relpath;
		}

		@Override
		public boolean isData() {
			return isData;
		}

		@Override
		public @Nullable String getAlias() {
			return alias;
		}

		@Override
		public <JsonNode> Map<String, JsonNode> getImportMetadata(JsonProvider<JsonNode> jsonProvider) {
			return evaluateMetadata(jsonProvider, importMetadataExpr);
		}
	}
}
