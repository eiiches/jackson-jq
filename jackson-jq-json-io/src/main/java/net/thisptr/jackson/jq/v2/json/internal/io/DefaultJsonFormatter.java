package net.thisptr.jackson.jq.v2.json.internal.io;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.NumberType;

/**
 * Formats JSON values using jq-style notation for computed floating-point numbers.
 * <p>
 * Nesting is walked with an explicit stack rather than by recursion, so a value is only ever as
 * expensive to write as it is to hold: a million levels deep costs a million {@link Frame}s on the
 * heap and no Java stack at all.
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

	/**
	 * One container that has been opened and not yet closed: what is left to write inside it, and the
	 * bracket that ends it.
	 *
	 * @param <N> the provider's node type
	 */
	private static final class Frame<N> {
		// A provider hands out an array's elements and an object's members as different iterator
		// types, so exactly one of these is set.
		private final @Nullable Iterator<N> elements;
		private final @Nullable Iterator<Map.Entry<String, N>> members;
		private final char close;

		/**
		 * Whether a child has been written already, and so whether a comma is due before the next one.
		 */
		private boolean written;

		private Frame(@Nullable Iterator<N> elements, @Nullable Iterator<Map.Entry<String, N>> members, char close) {
			this.elements = elements;
			this.members = members;
			this.close = close;
		}

		private static <N> Frame<N> array(Iterator<N> elements) {
			return new Frame<>(elements, null, ']');
		}

		private static <N> Frame<N> object(Iterator<Map.Entry<String, N>> members) {
			return new Frame<>(null, members, '}');
		}

		private boolean hasNext() {
			return elements != null ? elements.hasNext() : Objects.requireNonNull(members).hasNext();
		}

		/**
		 * Advances to the next child, writing an object member's key and colon ahead of its value so
		 * the caller only has to deal with the value itself.
		 */
		private N next(StringBuilder result, FormatOptions options) {
			if (elements != null)
				return elements.next();
			Map.Entry<String, N> member = Objects.requireNonNull(members).next();
			appendString(result, options, member.getKey());
			append(result, options, ':');
			return member.getValue();
		}
	}

	static <N> String format(JsonProvider<N> provider, N node, FormatOptions options) {
		StringBuilder result = new StringBuilder();
		Deque<Frame<N>> open = new ArrayDeque<>();
		appendValue(result, provider, node, options, open);
		while (!open.isEmpty()) {
			Frame<N> frame = open.element();
			if (!frame.hasNext()) {
				append(result, options, frame.close);
				open.pop();
				continue;
			}
			if (frame.written)
				append(result, options, ',');
			frame.written = true;
			appendValue(result, provider, frame.next(result, options), options, open);
		}
		return result.toString();
	}

	/**
	 * Writes a scalar outright, or opens a container and pushes the frame that will finish it.
	 */
	private static <N> void appendValue(StringBuilder result, JsonProvider<N> provider, N node, FormatOptions options,
			Deque<Frame<N>> open) {
		switch (provider.getNodeType(node)) {
			case NULL -> append(result, options, "null");
			case BOOLEAN -> append(result, options, provider.getBoolean(node) ? "true" : "false");
			case STRING -> appendString(result, options, provider.getString(node));
			case NUMBER -> appendNumber(result, provider, node, options);
			case BINARY ->
					appendString(result, options, Base64.getEncoder().encodeToString(provider.getBinaryAsByteArray(node)));
			case ARRAY -> {
				append(result, options, '[');
				open.push(Frame.array(provider.getArrayElements(node)));
			}
			case OBJECT -> {
				append(result, options, '{');
				open.push(Frame.object(provider.getObjectMembers(node)));
			}
		}
	}

	private static <N> void appendNumber(StringBuilder result, JsonProvider<N> provider, N node, FormatOptions options) {
		if (options.getRoundNumbersToDouble()) {
			appendDouble(result, options, provider.getNumberAsDoubleRounded(node));
			return;
		}
		NumberType type = provider.getNumberType(node);
		if (type == NumberType.DOUBLE || type == NumberType.FLOAT) {
			appendDouble(result, options, provider.getNumberAsDoubleRounded(node));
			return;
		}
		@Nullable BigDecimal exact = provider.getNumberAsBigDecimalExact(node);
		if (exact != null) {
			if (type == NumberType.BIG_DECIMAL || type == NumberType.UNKNOWN)
				appendNumberText(result, options, exact.toString());
			else
				append(result, options, exact.toBigIntegerExact().toString());
			return;
		}
		appendDouble(result, options, provider.getNumberAsDoubleRounded(node));
	}

	private static void appendNumberText(StringBuilder result, FormatOptions options, String text) {
		append(result, options, options.getLowerCaseDecimalExponent() ? text.replace('E', 'e') : text);
	}

	private static void appendDouble(StringBuilder result, FormatOptions options, double value) {
		if (Double.isNaN(value)) {
			append(result, options, "null");
			return;
		}
		if (Double.isInfinite(value)) {
			append(result, options, value > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308");
			return;
		}
		if (value == 0) {
			// jq prints a negative zero as -0, at every version. BigDecimal has no negative zero, so the
			// sign has to be read off the double before converting below.
			if (Math.copySign(1.0, value) < 0)
				append(result, options, '-');
			append(result, options, '0');
			return;
		}
		BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
		int digits = decimal.precision();
		int decimalPoint = digits - decimal.scale();
		if (decimalPoint > -4 && decimalPoint <= digits + 15) {
			append(result, options, decimal.toPlainString());
			return;
		}
		String significand = decimal.unscaledValue().abs().toString();
		if (decimal.signum() < 0)
			append(result, options, '-');
		append(result, options, significand.charAt(0));
		if (significand.length() > 1) {
			append(result, options, '.');
			append(result, options, significand, 1, significand.length());
		}
		int exponent = decimalPoint - 1;
		append(result, options, 'e');
		append(result, options, exponent < 0 ? '-' : '+');
		int magnitude = Math.abs(exponent);
		if (magnitude < 10)
			append(result, options, '0');
		append(result, options, Integer.toString(magnitude));
	}

	private static void appendString(StringBuilder result, FormatOptions options, String value) {
		append(result, options, '"');
		for (int i = 0; i < value.length(); ++i) {
			char ch = value.charAt(i);
			switch (ch) {
				case '"' -> append(result, options, "\\\"");
				case '\\' -> append(result, options, "\\\\");
				case '\b' -> append(result, options, "\\b");
				case '\f' -> append(result, options, "\\f");
				case '\n' -> append(result, options, "\\n");
				case '\r' -> append(result, options, "\\r");
				case '\t' -> append(result, options, "\\t");
				default -> {
					if (ch < 0x20) {
						append(result, options, "\\u00");
						append(result, options, Character.forDigit(ch >>> 4, 16));
						append(result, options, Character.forDigit(ch & 0xf, 16));
					} else {
						append(result, options, ch);
					}
				}
			}
		}
		append(result, options, '"');
	}

	private static void append(StringBuilder result, FormatOptions options, String piece) {
		checkLength(result, options, piece.length());
		result.append(piece);
	}

	private static void append(StringBuilder result, FormatOptions options, char piece) {
		checkLength(result, options, 1);
		result.append(piece);
	}

	private static void append(StringBuilder result, FormatOptions options, CharSequence piece, int start, int end) {
		checkLength(result, options, end - start);
		result.append(piece, start, end);
	}

	/**
	 * Stops before the output grows past the configured cap, rather than after. Every write goes
	 * through here, so a value too large to serialize is rejected having built only the cap's worth of
	 * it -- and since each level of nesting writes at least one bracket, the cap bounds how deep the
	 * frame stack can grow too.
	 */
	private static void checkLength(StringBuilder result, FormatOptions options, int added) {
		long length = (long) result.length() + added;
		if (length > options.getMaxLength())
			throw new JsonSizeExceededException(JsonSizeExceededException.Kind.STRING, length, options.getMaxLength());
	}
}
