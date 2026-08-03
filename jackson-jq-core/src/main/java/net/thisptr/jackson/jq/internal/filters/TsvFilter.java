package net.thisptr.jackson.jq.internal.filters;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("@tsv/0")
public class TsvFilter extends AbstractSvFilter {
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
