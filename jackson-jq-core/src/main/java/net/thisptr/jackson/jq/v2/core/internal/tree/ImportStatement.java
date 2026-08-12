package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ImportStatement<JsonNode> {
	public final String path;
	public final boolean dollarImport;
	public final String name;
	private final @Nullable Expression<JsonNode> metadataExpr;
	private @Nullable JsonNode metadata;
	private boolean metadataEvaluated = false;

	public ImportStatement(String path, boolean dollarImport, String name, @Nullable Expression<JsonNode> metadataExpr) {
		this.path = path;
		this.dollarImport = dollarImport;
		this.name = name;
		this.metadataExpr = metadataExpr;
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
