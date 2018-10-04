package net.thisptr.jackson.jq.internal.functions;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("@html/0")
public class AtHtmlFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(final String text) {
		final StringBuilder builder = new StringBuilder();
		for (final char ch : text.toCharArray()) {
			switch (ch) {
				case '<':
					builder.append("&lt;");
					break;
				case '>':
					builder.append("&gt;");
					break;
				case '\'':
					builder.append("&apos;");
					break;
				case '"':
					builder.append("&quot;");
					break;
				case '&':
					builder.append("&amp;");
					break;
				case '\0':
					builder.append("\\0");
					break;
				default:
					builder.append(ch);
					break;
			}
		}
		return builder.toString();
	}
}
