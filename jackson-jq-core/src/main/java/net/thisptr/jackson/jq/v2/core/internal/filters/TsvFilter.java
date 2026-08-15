package net.thisptr.jackson.jq.v2.core.internal.filters;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "@tsv", nargs = 0)
public class TsvFilter extends AbstractSvFilter {
	@Override
	protected void appendEscaped(StringBuilder builder, String text) {
		for (int i = 0; i < text.length(); ++i) {
			char ch = text.charAt(i);
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
	protected void appendSeparator(StringBuilder builder) {
		builder.append('\t');
	}

	@Override
	protected String name() {
		return "tsv";
	}
}
