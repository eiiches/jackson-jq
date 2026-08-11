package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("@html/0")
public class AtHtmlFunction<JsonNode> extends AbstractAtFormattingFunction<JsonNode> {
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
