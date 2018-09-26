package net.thisptr.jackson.jq.internal.filters;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

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
