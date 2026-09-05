package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import java.math.BigDecimal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * A numeric literal, holding the source text exactly as it was written. The parser makes no
 * attempt to decide what number the text denotes; that happens when the node is turned into an
 * {@link net.thisptr.jackson.jq.v2.spi.Expression}, or here in {@link #value(JsonProvider)}.
 */
public class NumericLiteralAstNode extends ValueLiteralAstNode {
	private final String text;

	public NumericLiteralAstNode(String text) {
		this.text = text;
	}

	/**
	 * Returns the literal exactly as it appeared in the query.
	 */
	public String text() {
		return text;
	}

	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createNumber(new BigDecimal(text));
	}

	@Override
	public String toString() {
		return text;
	}
}
