package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.utils.ExpressionUtils;

public class ImportStatement<JsonNode> {
	public final String path;
	public final boolean dollarImport;
	public final String name;
	private final Expression<JsonNode> metadataExpr;
	private JsonNode metadata;
	private boolean metadataEvaluated = false;

	public ImportStatement(final String path, final boolean dollarImport, final String name, final Expression<JsonNode> metadataExpr) {
		this.path = path;
		this.dollarImport = dollarImport;
		this.name = name;
		this.metadataExpr = metadataExpr;
	}

	public JsonNode getMetadata(final JsonProvider<JsonNode> jsonProvider) {
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

	public String toString(final JsonProvider<JsonNode> jsonProvider) {
		final StringBuilder s = new StringBuilder();
		s.append("import ");
		s.append(jsonProvider.createString(path).toString());
		s.append(" as ");
		if (dollarImport)
			s.append('$');
		s.append(name);
		final JsonNode md = getMetadata(jsonProvider);
		if (md != null) {
			s.append(' ');
			s.append(md);
		}
		return s.toString();
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
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
