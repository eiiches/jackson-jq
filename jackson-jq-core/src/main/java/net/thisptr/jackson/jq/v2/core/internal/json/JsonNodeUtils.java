package net.thisptr.jackson.jq.v2.core.internal.json;

import java.util.List;
import java.util.Locale;

import net.thisptr.jackson.jq.v2.core.internal.commons.collection.Lists;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitChecks;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.FormatOptions;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonSizeExceededException;
import net.thisptr.jackson.jq.v2.json.internal.io.ParseOptions;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class JsonNodeUtils {
	private static final FormatOptions FORMAT_OPTIONS = FormatOptions.newBuilder().build();
	private static final FormatOptions LEGACY_FORMAT_OPTIONS = FormatOptions.newBuilder()
			.setRoundNumbersToDouble(true)
			.build();
	private static final ParseOptions LEGACY_PARSE_OPTIONS = ParseOptions.newBuilder()
			.setRoundNumbersToDouble(true)
			.build();

	private JsonNodeUtils() {
	}

	public static <JsonNode> boolean asBoolean(JsonProvider<JsonNode> jsonProvider, JsonNode n) {
		if (jsonProvider.isNull(n))
			return false;
		if (jsonProvider.isBoolean(n))
			return jsonProvider.getBoolean(n);
		return true;
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, long value) {
		if (((int) value) == value)
			return jsonProvider.createNumber((int) value);
		return jsonProvider.createNumber(value);
	}

	public static <JsonNode> JsonNode asNumericNode(JsonProvider<JsonNode> jsonProvider, double value) {
		// Narrowing -0.0 to an int would drop the sign of the zero, which jq prints as -0. Everything else
		// survives the narrowing below unchanged.
		if (value == 0.0 && Math.copySign(1.0, value) < 0)
			return jsonProvider.createNumber(value);
		if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE && value == Math.rint(value))
			return jsonProvider.createNumber((int) value);
		if (value >= Long.MIN_VALUE && value < 0x1p63 && value == Math.rint(value))
			return asNumericNode(jsonProvider, (long) value);
		return jsonProvider.createNumber(value);
	}

	public static <JsonNode> JsonNode asArrayNode(JsonProvider<JsonNode> jsonProvider, List<JsonNode> values) {
		return jsonProvider.createArray(values);
	}

	public static <JsonNode> List<JsonNode> asArrayList(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return Lists.newArrayList(jsonProvider.getArrayElements(in));
	}

	public static <JsonNode> String typeOf(JsonProvider<JsonNode> jsonProvider, JsonNode in) {
		return jsonProvider.getNodeType(in).toString().toLowerCase(Locale.ROOT);
	}

	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		return JsonCodec.format(jsonProvider, node);
	}

	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node, Version version) {
		if (version.compareTo(Versions.JQ_1_7) < 0)
			return JsonCodec.format(jsonProvider, node, LEGACY_FORMAT_OPTIONS);
		return JsonCodec.format(jsonProvider, node);
	}

	/**
	 * Serializes a node as the given jq version writes it, giving up as soon as the text would breach
	 * the invocation's string budget.
	 * <p>
	 * Checking as the text grows, rather than measuring it afterwards, is what keeps a value nobody
	 * could use from being serialized in full first.
	 *
	 * @param <JsonNode> the JSON node type
	 * @param jsonProvider the JSON provider that owns {@code node}
	 * @param node the node to serialize
	 * @param version the jq compatibility version, which selects how numbers are written
	 * @param limits the invocation's limits
	 * @return the serialized text
	 * @throws RuntimeLimitExceededException if the text would exceed {@link RuntimeLimits#getMaxStringLength()}
	 */
	public static <JsonNode> String toString(JsonProvider<JsonNode> jsonProvider, JsonNode node, Version version, RuntimeLimits limits) {
		try {
			return JsonCodec.format(jsonProvider, node, formatOptions(version, limits.getMaxStringLength()));
		} catch (JsonSizeExceededException tooLong) {
			RuntimeLimitChecks.checkStringLength(limits, tooLong.getSize());
			throw tooLong; // Unreachable: the formatter only gives up once the budget is already breached.
		}
	}

	private static FormatOptions formatOptions(Version version, int maxLength) {
		FormatOptions base = version.compareTo(Versions.JQ_1_7) < 0 ? LEGACY_FORMAT_OPTIONS : FORMAT_OPTIONS;
		if (maxLength == Integer.MAX_VALUE)
			return base;
		return base.toBuilder().setMaxLength(maxLength).build();
	}

	/**
	 * Returns the options for reading JSON the way the given jq version holds it.
	 * <p>
	 * jq kept no decimal literal before 1.7 -- every number was a {@code double} -- so rounding has to
	 * happen as the text is read, not only when it is printed. Reading exactly and rounding on output
	 * would spell the number right but still compare two literals that share a {@code double} as
	 * different values.
	 */
	public static ParseOptions parseOptions(Version version) {
		return roundsParsedNumbersToDouble(version) ? LEGACY_PARSE_OPTIONS : ParseOptions.newBuilder().build();
	}

	/**
	 * Returns whether the given jq version holds every number it reads as a {@code double}, keeping no
	 * decimal literal. True before 1.7. Reading a numeral anywhere -- JSON text, {@code tonumber} -- has
	 * to obey this, or two numerals that share a {@code double} would wrongly stay distinct values.
	 */
	public static boolean roundsParsedNumbersToDouble(Version version) {
		return version.compareTo(Versions.JQ_1_7) < 0;
	}

	/**
	 * Returns true if the node is a value node (not a container node like array or object).
	 */
	public static <JsonNode> boolean isValueNode(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		return type != JsonNodeType.ARRAY && type != JsonNodeType.OBJECT;
	}

}
