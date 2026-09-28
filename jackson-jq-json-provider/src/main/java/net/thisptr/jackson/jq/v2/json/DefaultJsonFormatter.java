package net.thisptr.jackson.jq.v2.json;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

final class DefaultJsonFormatter {
	private DefaultJsonFormatter() {
	}

	static <N> String format(JsonProvider<N> provider, N node, FormatOptions options) {
		StringBuilder result = new StringBuilder();
		append(result, provider, node, options);
		return result.toString();
	}

	private static <N> void append(StringBuilder result, JsonProvider<N> provider, N node, FormatOptions options) {
		switch (provider.getNodeType(node)) {
			case NULL -> result.append("null");
			case BOOLEAN -> result.append(provider.getBoolean(node));
			case STRING -> appendString(result, provider.getString(node));
			case NUMBER -> appendNumber(result, provider, node, options);
			case BINARY ->
					appendString(result, Base64.getEncoder().encodeToString(provider.getBinaryAsByteArray(node)));
			case ARRAY -> {
				result.append('[');
				Iterator<N> values = provider.getArrayElements(node);
				while (values.hasNext()) {
					append(result, provider, values.next(), options);
					if (values.hasNext())
						result.append(',');
				}
				result.append(']');
			}
			case OBJECT -> {
				result.append('{');
				Iterator<Map.Entry<String, N>> members = provider.getObjectMembers(node);
				while (members.hasNext()) {
					Map.Entry<String, N> member = members.next();
					appendString(result, member.getKey());
					result.append(':');
					append(result, provider, member.getValue(), options);
					if (members.hasNext())
						result.append(',');
				}
				result.append('}');
			}
		}
	}

	private static <N> void appendNumber(StringBuilder result, JsonProvider<N> provider, N node, FormatOptions options) {
		if (options.getRoundNumbersToDouble()) {
			appendDouble(result, provider.getNumberAsDoubleRounded(node), options);
			return;
		}
		NumberType type = provider.getNumberType(node);
		if (type == NumberType.DOUBLE || type == NumberType.FLOAT) {
			appendDouble(result, provider.getNumberAsDoubleRounded(node), options);
			return;
		}
		@Nullable BigDecimal exact = provider.getNumberAsBigDecimalExact(node);
		if (exact != null) {
			if (type == NumberType.BIG_DECIMAL || type == NumberType.UNKNOWN)
				appendNumberText(result, exact.toString(), options);
			else
				result.append(exact.toBigIntegerExact());
			return;
		}
		appendDouble(result, provider.getNumberAsDoubleRounded(node), options);
	}

	private static void appendNumberText(StringBuilder result, String text, FormatOptions options) {
		result.append(options.getLowerCaseExponent() ? text.replace('E', 'e') : text);
	}

	private static void appendDouble(StringBuilder result, double value, FormatOptions options) {
		if (Double.isNaN(value)) {
			result.append("null");
			return;
		}
		if (Double.isInfinite(value)) {
			result.append(value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308");
			return;
		}
		if (value == 0 || (value == Math.floor(value) && value >= Long.MIN_VALUE && value < 0x1p63)) {
			result.append((long) value);
			return;
		}
		@Var String text = Double.toString(value).replace('e', 'E');
		int exponent = text.indexOf('E');
		if (exponent >= 0 && text.charAt(exponent + 1) != '-')
			text = text.substring(0, exponent + 1) + "+" + text.substring(exponent + 1);
		appendNumberText(result, text, options);
	}

	private static void appendString(StringBuilder result, String value) {
		result.append('"');
		for (int i = 0; i < value.length(); ++i) {
			char ch = value.charAt(i);
			switch (ch) {
				case '"' -> result.append("\\\"");
				case '\\' -> result.append("\\\\");
				case '\b' -> result.append("\\b");
				case '\f' -> result.append("\\f");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				default -> {
					if (ch < 0x20) {
						result.append("\\u00");
						result.append(Character.forDigit(ch >>> 4, 16));
						result.append(Character.forDigit(ch & 0xf, 16));
					} else {
						result.append(ch);
					}
				}
			}
		}
		result.append('"');
	}
}
