package net.thisptr.jackson.jq.v2.core.internal.ast.impls.literal;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

public class StringLiteralAstNode extends ValueLiteralAstNode {
	private String text;

	public StringLiteralAstNode(String text) {
		this.text = text;
	}

	/**
	 * Returns the raw string value (not as a JsonNode).
	 */
	public String value() {
		return text;
	}

	@Override
	public <JsonNode> JsonNode value(JsonProvider<JsonNode> jsonProvider) {
		return jsonProvider.createString(text);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
			switch (ch) {
				case '\\':
					builder.append("\\\\");
					break;
				case '"':
					builder.append("\\\"");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				case '\n':
					builder.append("\\n");
					break;
				default:
					builder.append(ch);
			}
		}
		builder.append('"');
		return builder.toString();
	}
}
