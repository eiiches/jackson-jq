package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "@html", nargs = 0)
public class AtHtmlFunction extends AbstractAtFormattingFunction {
	@Override
	public String convert(String text) {
		StringBuilder builder = new StringBuilder();
		for (char ch : text.toCharArray()) {
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
