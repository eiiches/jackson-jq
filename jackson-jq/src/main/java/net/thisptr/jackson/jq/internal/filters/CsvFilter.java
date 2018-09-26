package net.thisptr.jackson.jq.internal.filters;

import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("@csv/0")
public class CsvFilter extends AbstractSvFilter {
	@Override
	protected void appendEscaped(final StringBuilder builder, final String text) {
		builder.append('"');
		for (int i = 0; i < text.length(); ++i) {
			final char ch = text.charAt(i);
			switch (ch) {
				case '"':
					builder.append("\"\"");
					break;
				default:
					builder.append(ch);
			}
		}
		builder.append('"');
	}

	@Override
	protected void appendSeparator(final StringBuilder builder) {
		builder.append(',');
	}

	@Override
	protected String name() {
		return "csv";
	}
}
