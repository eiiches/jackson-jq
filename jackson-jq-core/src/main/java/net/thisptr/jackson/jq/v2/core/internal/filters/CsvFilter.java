package net.thisptr.jackson.jq.v2.core.internal.filters;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "@csv", nargs = 0)
public class CsvFilter extends AbstractSvFilter {
	@Override
	protected void appendEscaped(StringBuilder builder, String text) {
		builder.append('"');
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
			switch (ch) {
				case '"':
					builder.append("\"\"");
					break;
				case '\0':
					builder.append("\\0");
					break;
				default:
					builder.append(ch);
			}
		}
		builder.append('"');
	}

	@Override
	protected void appendSeparator(StringBuilder builder) {
		builder.append(',');
	}

	@Override
	protected String name() {
		return "csv";
	}
}
