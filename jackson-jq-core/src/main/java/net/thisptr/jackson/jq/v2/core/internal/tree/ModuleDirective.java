package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.utils.ExpressionUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;

public class ModuleDirective<JsonNode> {
	private final Expression<JsonNode> metadataExpr;
	private @Nullable JsonNode metadata;
	private boolean metadataEvaluated = false;

	public ModuleDirective(Expression<JsonNode> metadataExpr) {
		this.metadataExpr = metadataExpr;
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
