package net.thisptr.jackson.jq.v2.core.internal.filters;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("@tsv/0")
public class TsvFilter<JsonNode> extends AbstractSvFilter<JsonNode> {
	@Override
	protected void appendEscaped(final StringBuilder builder, final String text) {
		for (int i = 0; i < text.length(); ++i) {
			final char ch = text.charAt(i);
			switch (ch) {
				case '\t':
					builder.append("\\t");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				case '\0':
					builder.append("\\0");
					break;
				default:
					builder.append(ch);
			}
		}
	}

	@Override
	protected void appendSeparator(final StringBuilder builder) {
		builder.append('\t');
	}

	@Override
	protected String name() {
		return "tsv";
	}
}
