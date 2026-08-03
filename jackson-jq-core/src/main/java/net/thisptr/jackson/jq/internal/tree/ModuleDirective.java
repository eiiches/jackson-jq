package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.utils.ExpressionUtils;

public class ModuleDirective {
	private final JsonNode metadata;

	public ModuleDirective(final Expression metadataExr) {
		this.metadata = ExpressionUtils.evaluateLiteralExpression(metadataExr);
		if (metadata == null)
			throw new IllegalArgumentException("Module metadata must be constant");
		if (!metadata.isObject())
			throw new IllegalArgumentException("Module metadata must be an object");
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append("module ");
		s.append(metadata);
		return s.toString();
	}
}
