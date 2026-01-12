package net.thisptr.jackson.jq.internal.tree;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.internal.utils.ExpressionUtils;

public class ModuleDirective<JsonNode> {
	private final Expression<JsonNode> metadataExpr;
	private JsonNode metadata;
	private boolean metadataEvaluated = false;

	public ModuleDirective(final Expression<JsonNode> metadataExpr) {
		this.metadataExpr = metadataExpr;
	}

	public JsonNode getMetadata(final JsonProvider<JsonNode> jsonProvider) {
		if (!metadataEvaluated) {
			this.metadata = ExpressionUtils.evaluateLiteralExpression(jsonProvider, metadataExpr);
			if (metadata == null)
				throw new IllegalArgumentException("Module metadata must be constant");
			if (jsonProvider.getNodeType(metadata) != JsonNodeType.OBJECT)
				throw new IllegalArgumentException("Module metadata must be an object");
			metadataEvaluated = true;
		}
		return metadata;
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append("module {...}");
		return s.toString();
	}
}
