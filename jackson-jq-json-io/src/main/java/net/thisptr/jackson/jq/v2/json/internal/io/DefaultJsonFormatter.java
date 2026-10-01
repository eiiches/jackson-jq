package net.thisptr.jackson.jq.v2.json.internal.io;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * Formats JSON values using jq-style notation for computed floating-point numbers.
 * <p>
 * jq's exponent letter depends on whether a number is an unchanged decimal literal or a
 * computed double:
 * <table>
 * <caption>Exponent letter used by jq</caption>
 * <tr><th scope="col">jq version</th><th scope="col">Decimal literal</th><th scope="col">Computed double</th></tr>
 * <tr><td>{@code [1.5, 1.7)}</td><td>{@code e}</td><td>{@code e}</td></tr>
 * <tr><td>{@code [1.7, )}</td><td>{@code E}</td><td>{@code e}</td></tr>
 * </table>
 */
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
			appendDouble(result, provider.getNumberAsDoubleRounded(node));
			return;
		}
		NumberType type = provider.getNumberType(node);
		if (type == NumberType.DOUBLE || type == NumberType.FLOAT) {
			appendDouble(result, provider.getNumberAsDoubleRounded(node));
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
		appendDouble(result, provider.getNumberAsDoubleRounded(node));
	}

	private static void appendNumberText(StringBuilder result, String text, FormatOptions options) {
		result.append(options.getLowerCaseDecimalExponent() ? text.replace('E', 'e') : text);
	}

	private static void appendDouble(StringBuilder result, double value) {
		if (Double.isNaN(value)) {
			result.append("null");
			return;
		}
		if (Double.isInfinite(value)) {
			result.append(value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308");
			return;
		}
		if (value == 0) {
			// jq prints a negative zero as -0, at every version. BigDecimal has no negative zero, so the
			// sign has to be read off the double before converting below.
			if (Math.copySign(1.0, value) < 0)
				result.append('-');
			result.append('0');
			return;
		}
		BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
		int digits = decimal.precision();
		int decimalPoint = digits - decimal.scale();
		if (decimalPoint > -4 && decimalPoint <= digits + 15) {
			result.append(decimal.toPlainString());
			return;
		}
		String significand = decimal.unscaledValue().abs().toString();
		if (decimal.signum() < 0)
			result.append('-');
		result.append(significand.charAt(0));
		if (significand.length() > 1)
			result.append('.').append(significand, 1, significand.length());
		int exponent = decimalPoint - 1;
		result.append('e').append(exponent < 0 ? '-' : '+');
		int magnitude = Math.abs(exponent);
		if (magnitude < 10)
			result.append('0');
		result.append(magnitude);
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
