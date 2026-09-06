package net.thisptr.jackson.jq.v2.json;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for {@link JsonProvider} implementations.
 * Each provider implementation should extend this class and implement {@link #getProvider()}.
 *
 * @param <T> The JSON node type used by the provider
 */
public interface JsonProviderContractTest<T> {

	/**
	 * Returns the JsonProvider instance to test.
	 */
	JsonProvider<T> getProvider();

	static <T> Map<String, T> mapOf(String k1, T v1, String k2, T v2) {
		Map<String, T> map = new LinkedHashMap<>();
		map.put(k1, v1);
		map.put(k2, v2);
		return map;
	}

	static <T> Map<String, T> mapOf(String k1, T v1, String k2, T v2, String k3, T v3) {
		Map<String, T> map = new LinkedHashMap<>();
		map.put(k1, v1);
		map.put(k2, v2);
		map.put(k3, v3);
		return map;
	}


	// ===================
	// Node Creation Tests
	// ===================

	@Test
	default void testCreateNull() {
		T node = getProvider().createNull();
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NULL);
	}

	@Test
	default void testCreateBoolean() {
		T trueNode = getProvider().createBoolean(true);
		assertThat(getProvider().getNodeType(trueNode)).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(getProvider().getBoolean(trueNode)).isTrue();

		T falseNode = getProvider().createBoolean(false);
		assertThat(getProvider().getNodeType(falseNode)).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(getProvider().getBoolean(falseNode)).isFalse();
	}

	@Test
	default void testCreateNumberFromInt() {
		T node = getProvider().createNumber(42);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNumberAsIntExact(node)).isEqualTo(42);
		assertThat(getProvider().getNumberAsLongExact(node)).isEqualTo(42L);
		assertThat(getProvider().getNumberAsDoubleRounded(node)).isEqualTo(42.0);
	}

	@Test
	default void testCreateNumberFromLong() {
		T node = getProvider().createNumber(9999999999L);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNumberAsLongExact(node)).isEqualTo(9999999999L);
		assertThat(getProvider().getNumberAsDoubleRounded(node)).isEqualTo(9999999999.0);
	}

	@Test
	default void testCreateNumberFromFloat() {
		T node = getProvider().createNumber(3.14f);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat((float) getProvider().getNumberAsDoubleRounded(node)).isEqualTo(3.14f);
	}

	@Test
	default void testCreateNumberFromDouble() {
		T node = getProvider().createNumber(3.14159);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNumberAsDoubleRounded(node)).isEqualTo(3.14159);
	}

	@Test
	default void testCreateNumberFromBigInteger() {
		BigInteger value = new BigInteger("123456789012345678901234567890");
		T node = getProvider().createNumber(value);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNumberAsDoubleRounded(node)).isEqualTo(value.doubleValue());
	}

	@Test
	default void testCreateNumberFromBigDecimal() {
		BigDecimal value = new BigDecimal("3.14159265358979323846264338327950288");
		T node = getProvider().createNumber(value);
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNumberAsDoubleRounded(node)).isEqualTo(value.doubleValue());
	}

	@Test
	default void testGetNumberAsBigDecimalExactIsLossless() {
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(42))).isEqualByComparingTo("42");

		// The whole point: this value is not representable as a double.
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(2871948651097801136L))).isEqualByComparingTo("2871948651097801136");

		BigInteger bigInteger = new BigInteger("123456789012345678901234567890");
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(bigInteger))).isEqualByComparingTo(new BigDecimal(bigInteger));

		BigDecimal bigDecimal = new BigDecimal("3.14159265358979323846264338327950288");
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(bigDecimal))).isEqualByComparingTo(bigDecimal);
	}

	@Test
	default void testGetNumberAsBigDecimalExactOnDoubleUsesShortestRepresentation() {
		// Not the exact binary expansion (0.1000000000000000055511151231257827...), which would stop
		// a computed 0.1 from comparing equal to the literal 0.1.
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(0.1))).isEqualByComparingTo("0.1");
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(-3.14))).isEqualByComparingTo("-3.14");
	}

	@Test
	default void testGetNumberAsBigDecimalExactOnNonFiniteReturnsNull() {
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(Double.NaN))).isNull();
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(Double.POSITIVE_INFINITY))).isNull();
		assertThat(getProvider().getNumberAsBigDecimalExact(getProvider().createNumber(Double.NEGATIVE_INFINITY))).isNull();
	}

	@Test
	default void testGetNumberAsBigDecimalExactOnNonNumberThrows() {
		assertThatThrownBy(() -> getProvider().getNumberAsBigDecimalExact(getProvider().createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsBigDecimalExact(getProvider().createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsBigDecimalExact(getProvider().createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsBigDecimalExact(getProvider().createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsBigDecimalExact(getProvider().createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testExactIntegralAccessorsReturnNullForFractionalValues() {
		T positive = getProvider().createNumber(1.9);
		T negative = getProvider().createNumber(-1.9);

		assertThat(getProvider().getNumberAsBigIntegerExact(positive)).isNull();
		assertThat(getProvider().getNumberAsBigIntegerExact(negative)).isNull();
		assertThat(getProvider().getNumberAsIntExact(positive)).isNull();
		assertThat(getProvider().getNumberAsIntExact(negative)).isNull();
		assertThat(getProvider().getNumberAsLongExact(positive)).isNull();
		assertThat(getProvider().getNumberAsLongExact(negative)).isNull();
	}

	@Test
	default void testExactIntegralAccessorsReturnNullOutOfRange() {
		assertThat(getProvider().getNumberAsIntExact(getProvider().createNumber(2147483648L))).isNull();
		assertThat(getProvider().getNumberAsIntExact(getProvider().createNumber(-2147483649L))).isNull();
		// Out of range because of the value, whatever the representation holding it.
		assertThat(getProvider().getNumberAsIntExact(getProvider().createNumber(1e15))).isNull();
		assertThat(getProvider().getNumberAsIntExact(getProvider().createNumber(-1e15))).isNull();
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(new BigInteger("123456789012345678901234567890")))).isNull();
	}

	@Test
	default void testIntegralAccessorsReturnNullForNonFiniteValues() {
		for (double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
			T node = getProvider().createNumber(value);
			assertThat(getProvider().getNumberAsBigIntegerExact(node)).isNull();
			assertThat(getProvider().getNumberAsBigIntegerTruncated(node)).isNull();
			assertThat(getProvider().getNumberAsIntExact(node)).isNull();
			assertThat(getProvider().getNumberAsLongExact(node)).isNull();
			assertThat(getProvider().getNumberAsIntTruncated(node)).isNull();
			assertThat(getProvider().getNumberAsLongTruncated(node)).isNull();
		}
	}

	@Test
	default void testIntegralAccessorsThrowOnNonNumber() {
		List<T> nonNumbers = Arrays.asList(
				getProvider().createString("42"),
				getProvider().createBoolean(true),
				getProvider().createNull(),
				getProvider().createArray(Collections.emptyList()),
				getProvider().createObject(Collections.emptyMap()));
		for (T node : nonNumbers) {
			assertThatThrownBy(() -> getProvider().getNumberAsBigIntegerExact(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> getProvider().getNumberAsBigIntegerTruncated(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> getProvider().getNumberAsIntExact(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> getProvider().getNumberAsLongExact(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> getProvider().getNumberAsIntTruncated(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> getProvider().getNumberAsLongTruncated(node)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	default void testGetNumberAsDoubleRoundedThrowsOnNonNumber() {
		// A NaN return therefore means the value is NaN, never that the node was the wrong type.
		assertThatThrownBy(() -> getProvider().getNumberAsDoubleRounded(getProvider().createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsDoubleRounded(getProvider().createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsDoubleRounded(getProvider().createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsDoubleRounded(getProvider().createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsDoubleRounded(getProvider().createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetNumberAsDoubleRoundedRoundsToNearest() {
		// Rounding, not truncation: the example documented by JsonProvider lands above the exact value.
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(2871948651097801136L))).isEqualTo(0x1.3ed9b0a7cec61p61);
		// These are exact halfway cases on opposite sides of an even significand.
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(9007199254740993L))).isEqualTo(0x1.0p53);
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(9007199254740995L))).isEqualTo(0x1.0000000000002p53);
	}

	@Test
	default void testGetNumberAsDoubleRoundedHandlesNonFiniteResults() {
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(new BigDecimal("1e400")))).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(new BigDecimal("-1e400")))).isEqualTo(Double.NEGATIVE_INFINITY);
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(Double.POSITIVE_INFINITY))).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(Double.NEGATIVE_INFINITY))).isEqualTo(Double.NEGATIVE_INFINITY);
		assertThat(getProvider().getNumberAsDoubleRounded(getProvider().createNumber(Double.NaN))).isNaN();
	}

	@Test
	default void testTruncatedIntegralAccessorsRoundTowardZero() {
		T positive = getProvider().createNumber(1.9);
		T negative = getProvider().createNumber(-1.9);

		assertThat(getProvider().getNumberAsBigIntegerTruncated(positive)).isEqualTo(BigInteger.ONE);
		assertThat(getProvider().getNumberAsBigIntegerTruncated(negative)).isEqualTo(BigInteger.ONE.negate());
		assertThat(getProvider().getNumberAsIntTruncated(positive)).isEqualTo(1);
		assertThat(getProvider().getNumberAsIntTruncated(negative)).isEqualTo(-1);
		assertThat(getProvider().getNumberAsLongTruncated(positive)).isEqualTo(1L);
		assertThat(getProvider().getNumberAsLongTruncated(negative)).isEqualTo(-1L);
	}

	@Test
	default void testBigIntegerConversionsHaveNoRangeLimit() {
		BigInteger hugeInteger = BigInteger.TEN.pow(400);
		assertThat(getProvider().getNumberAsBigIntegerExact(getProvider().createNumber(hugeInteger))).isEqualTo(hugeInteger);
		assertThat(getProvider().getNumberAsBigIntegerExact(getProvider().createNumber(new BigDecimal("1.000e400")))).isEqualTo(hugeInteger);
		assertThat(getProvider().getNumberAsBigIntegerExact(getProvider().createNumber(1e100))).isEqualTo(BigInteger.TEN.pow(100));
		assertThat(getProvider().getNumberAsBigIntegerExact(getProvider().createNumber(42.0f))).isEqualTo(BigInteger.valueOf(42));

		BigDecimal hugeFraction = new BigDecimal(hugeInteger).add(new BigDecimal("0.9"));
		assertThat(getProvider().getNumberAsBigIntegerTruncated(getProvider().createNumber(hugeFraction))).isEqualTo(hugeInteger);
		assertThat(getProvider().getNumberAsBigIntegerTruncated(getProvider().createNumber(hugeFraction.negate()))).isEqualTo(hugeInteger.negate());
		assertThat(getProvider().getNumberAsBigIntegerTruncated(getProvider().createNumber(0.9))).isEqualTo(BigInteger.ZERO);
		assertThat(getProvider().getNumberAsBigIntegerTruncated(getProvider().createNumber(-0.9))).isEqualTo(BigInteger.ZERO);
	}

	@Test
	default void testTruncatedIntChecksRangeAfterTruncation() {
		assertThat(getProvider().getNumberAsIntTruncated(getProvider().createNumber(2147483647.9))).isEqualTo(Integer.MAX_VALUE);
		assertThat(getProvider().getNumberAsIntTruncated(getProvider().createNumber(-2147483648.9))).isEqualTo(Integer.MIN_VALUE);
		assertThat(getProvider().getNumberAsIntTruncated(getProvider().createNumber(2147483648.0))).isNull();
		assertThat(getProvider().getNumberAsIntTruncated(getProvider().createNumber(-2147483649.0))).isNull();
	}

	@Test
	default void testTruncatedLongDoesNotLoseDoublePrecision() {
		long value = 9007199254740993L;
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(value))).isEqualTo(value);
	}

	@Test
	default void testLongConversionsBeyondExactDoubleIntegerBoundary() {
		double above = Math.nextUp(0x1p53);
		double below = Math.nextDown(-0x1p53);

		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(above))).isEqualTo(9_007_199_254_740_994L);
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(above))).isEqualTo(9_007_199_254_740_994L);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(below))).isEqualTo(-9_007_199_254_740_994L);
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(below))).isEqualTo(-9_007_199_254_740_994L);

		long positiveOdd = 9_007_199_254_740_995L;
		long negativeOdd = -9_007_199_254_740_995L;
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber((double) positiveOdd))).isEqualTo(9_007_199_254_740_996L);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber((double) negativeOdd))).isEqualTo(-9_007_199_254_740_996L);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(positiveOdd))).isEqualTo(positiveOdd);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(negativeOdd))).isEqualTo(negativeOdd);

		double largestLong = Math.nextDown(0x1p63);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(largestLong))).isEqualTo((long) largestLong);
		assertThat(getProvider().getNumberAsLongExact(getProvider().createNumber(0x1p63))).isNull();
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(0x1p63))).isNull();
	}

	@Test
	default void testTruncatedIntegralAccessorsReturnNullForUnrepresentableValues() {
		// A wrong node type still throws; only "no representative exists" is null.
		T text = getProvider().createString("1");
		assertThatThrownBy(() -> getProvider().getNumberAsIntTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberAsLongTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThat(getProvider().getNumberAsIntTruncated(getProvider().createNumber(Double.NaN))).isNull();
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(Double.POSITIVE_INFINITY))).isNull();
		assertThat(getProvider().getNumberAsLongTruncated(getProvider().createNumber(1e20))).isNull();
	}

	@Test
	default void testCreateString() {
		T node = getProvider().createString("hello");
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(getProvider().getString(node)).isEqualTo("hello");
	}

	@Test
	default void testCreateEmptyString() {
		T node = getProvider().createString("");
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(getProvider().getString(node)).isEqualTo("");
	}

	@Test
	default void testGetStringRejectsNonStrings() {
		List<T> nonStrings = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonStrings)
			assertThatThrownBy(() -> getProvider().getString(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetBooleanRejectsNonBooleans() {
		List<T> nonBooleans = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createNumber(0),
				getProvider().createString("abc"),
				getProvider().createNull());

		for (T node : nonBooleans)
			assertThatThrownBy(() -> getProvider().getBoolean(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testCreateBinary() {
		byte[] bytes = { 0, 1, 127, -128 };
		T node;
		try {
			node = getProvider().createBinary(bytes);
		} catch (UnsupportedOperationException e) {
			// Documented for a provider whose library has no binary node type. Every node it can
			// create is then non-binary, which testGetBinaryAsByteArrayRejectsNonBinary covers.
			return;
		}
		assertTypePredicates(node, JsonNodeType.BINARY);
		assertThat(getProvider().getBinaryAsByteArray(node)).isEqualTo(bytes);
	}

	@Test
	default void testGetBinaryAsByteArrayRejectsNonBinary() {
		// A provider with no binary node type rejects these the same way: none of them is binary.
		List<T> nonBinary = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createString("YWJj"), // valid base64, which Jackson 2 used to decode here
				getProvider().createNull());

		for (T node : nonBinary)
			assertThatThrownBy(() -> getProvider().getBinaryAsByteArray(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testCreateObject() {
		T node = getProvider().createObject(Collections.emptyMap());
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(getProvider().getObjectMemberCount(node)).isEqualTo(0);
	}

	@Test
	default void testCreateArray() {
		T node = getProvider().createArray(Collections.emptyList());
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(getProvider().getArrayLength(node)).isEqualTo(0);
	}

	@Test
	default void testCreateArrayFromValues() {
		T node = getProvider().createArray(Arrays.asList(getProvider().createNumber(1), getProvider().createString("two")));

		assertThat(getProvider().getArrayLength(node)).isEqualTo(2);
		assertThat(getProvider().getNumberAsIntExact(getProvider().getArrayElement(node, 0))).isEqualTo(1);
		assertThat(getProvider().getString(getProvider().getArrayElement(node, 1))).isEqualTo("two");
	}

	@Test
	default void testCreateObjectFromValues() {
		Map<String, T> values = new LinkedHashMap<>();
		values.put("one", getProvider().createNumber(1));
		values.put("two", getProvider().createString("two"));
		T node = getProvider().createObject(values);

		assertThat(getProvider().getObjectMemberCount(node)).isEqualTo(2);
		assertThat(getProvider().getNumberAsIntExact(getProvider().getObjectMemberOrThrow(node, "one"))).isEqualTo(1);
		assertThat(getProvider().getString(getProvider().getObjectMemberOrThrow(node, "two"))).isEqualTo("two");
	}

	@Test
	default void testGetArrayLengthRejectsNonArrays() {
		List<T> nonArrays = Arrays.asList(
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonArrays)
			assertThatThrownBy(() -> getProvider().getArrayLength(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetObjectMemberCountRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMemberCount(node)).isInstanceOf(IllegalArgumentException.class);
	}

	// ====================
	// Type Predicate Tests
	// ====================

	default void assertTypePredicates(T node, JsonNodeType expected) {
		assertThat(getProvider().getNodeType(node)).as("getNodeType(%s)", getProvider().format(node)).isEqualTo(expected);
		assertThat(getProvider().isObject(node)).as("isObject(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.OBJECT);
		assertThat(getProvider().isArray(node)).as("isArray(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.ARRAY);
		assertThat(getProvider().isString(node)).as("isString(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.STRING);
		assertThat(getProvider().isNumber(node)).as("isNumber(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.NUMBER);
		assertThat(getProvider().isBoolean(node)).as("isBoolean(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.BOOLEAN);
		assertThat(getProvider().isNull(node)).as("isNull(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.NULL);
		assertThat(getProvider().isBinary(node)).as("isBinary(%s)", getProvider().format(node)).isEqualTo(expected == JsonNodeType.BINARY);
	}

	/**
	 * Each predicate must answer exactly like the corresponding {@link JsonProvider#getNodeType(Object)}
	 * comparison. Providers are expected to override the defaults with their library's native check, so
	 * every predicate is exercised against every type, not just its own.
	 */
	@Test
	default void testTypePredicates() {
		Map<JsonNodeType, List<T>> samples = new LinkedHashMap<>();
		samples.put(JsonNodeType.NULL, Arrays.asList(
				getProvider().createNull(),
				getProvider().parse("null")));
		samples.put(JsonNodeType.BOOLEAN, Arrays.asList(
				getProvider().createBoolean(true),
				getProvider().createBoolean(false)));
		samples.put(JsonNodeType.NUMBER, Arrays.asList(
				getProvider().createNumber(42),
				getProvider().createNumber(9999999999L),
				getProvider().createNumber(3.14f),
				getProvider().createNumber(3.14159),
				getProvider().createNumber(BigInteger.ONE),
				getProvider().createNumber(BigDecimal.ONE)));
		samples.put(JsonNodeType.STRING, Arrays.asList(
				getProvider().createString(""),
				getProvider().createString("hello")));
		samples.put(JsonNodeType.ARRAY, Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createArray(Collections.singletonList(getProvider().createNumber(1)))));
		samples.put(JsonNodeType.OBJECT, Arrays.asList(
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createObject(mapOf("a", getProvider().createNumber(1), "b", getProvider().createNull()))));

		for (Map.Entry<JsonNodeType, List<T>> entry : samples.entrySet())
			for (T node : entry.getValue())
				assertTypePredicates(node, entry.getKey());
	}

	// ===================
	// Object Operations
	// ===================

	@Test
	default void testGetObjectMembers() {
		T obj = getProvider().createObject(mapOf("x", getProvider().createNumber(10), "y", getProvider().createNumber(20)));

		List<String> keys = new ArrayList<>();
		List<Integer> values = new ArrayList<>();
		Iterator<Map.Entry<String, T>> it = getProvider().getObjectMembers(obj);
		while (it.hasNext()) {
			Map.Entry<String, T> entry = it.next();
			keys.add(entry.getKey());
			values.add(getProvider().getNumberAsIntExact(entry.getValue()));
		}

		assertThat(keys).containsExactlyInAnyOrder("x", "y");
		assertThat(values).containsExactlyInAnyOrder(10, 20);
	}

	@Test
	default void testGetObjectMembersRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMembers(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetObjectMemberNames() {
		T obj = getProvider().createObject(mapOf("foo", getProvider().createNull(), "bar", getProvider().createNull()));

		List<String> names = new ArrayList<>();
		Iterator<String> it = getProvider().getObjectMemberNames(obj);
		while (it.hasNext()) {
			names.add(it.next());
		}

		assertThat(names).containsExactlyInAnyOrder("foo", "bar");
	}

	@Test
	default void testGetObjectMemberNamesRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMemberNames(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetObjectMemberValues() {
		T obj = getProvider().createObject(mapOf("a", getProvider().createNumber(1), "b", getProvider().createNumber(2), "c", getProvider().createNumber(3)));

		List<Integer> values = new ArrayList<>();
		Iterator<T> it = getProvider().getObjectMemberValues(obj);
		while (it.hasNext()) {
			values.add(getProvider().getNumberAsIntExact(it.next()));
		}

		assertThat(values).containsExactly(1, 2, 3);
	}

	@Test
	default void testGetObjectMemberValuesRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMemberValues(node)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetObjectFieldAndHasObjectMember() {
		Map<String, T> map = new LinkedHashMap<>();
		map.put("str", getProvider().createString("hello"));
		map.put("nul", getProvider().createNull());
		T obj = getProvider().createObject(map);

		// Present non-null field
		assertThat(getProvider().hasObjectMember(obj, "str")).isTrue();
		T strVal = getProvider().getObjectMember(obj, "str");
		assertThat(strVal).isNotNull();
		assertThat(getProvider().getString(Objects.requireNonNull(strVal))).isEqualTo("hello");
		assertThat(getProvider().getObjectMemberOrThrow(obj, "str")).isNotNull();

		// Present explicit JSON null field
		assertThat(getProvider().hasObjectMember(obj, "nul")).isTrue();
		T nullVal = getProvider().getObjectMember(obj, "nul");
		assertThat(nullVal).isNotNull();
		assertThat(getProvider().getNodeType(Objects.requireNonNull(nullVal))).isEqualTo(JsonNodeType.NULL);
		assertThat(getProvider().getObjectMemberOrThrow(obj, "nul")).isNotNull();

		// Absent field
		assertThat(getProvider().hasObjectMember(obj, "missing")).isFalse();
		assertThat(getProvider().getObjectMember(obj, "missing")).isNull();
		assertThatThrownBy(() -> getProvider().getObjectMemberOrThrow(obj, "missing")).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	default void testGetObjectMemberRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMember(node, "foo")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testHasObjectMemberRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().hasObjectMember(node, "foo")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetObjectMemberOrThrowRejectsNonObjects() {
		List<T> nonObjects = Arrays.asList(
				getProvider().createArray(Collections.emptyList()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonObjects)
			assertThatThrownBy(() -> getProvider().getObjectMemberOrThrow(node, "foo")).isInstanceOf(IllegalArgumentException.class);
	}

	// ===================
	// Array Operations
	// ===================

	@Test
	default void testGetArrayElements() {
		T arr = getProvider().createArray(Arrays.asList(getProvider().createString("a"), getProvider().createString("b"), getProvider().createString("c")));

		List<String> elements = new ArrayList<>();
		Iterator<T> it = getProvider().getArrayElements(arr);
		while (it.hasNext()) {
			elements.add(getProvider().getString(it.next()));
		}

		assertThat(elements).containsExactly("a", "b", "c");
	}

	@Test
	default void testGetArrayElement() {
		T arr = getProvider().createArray(Arrays.asList(getProvider().createString("a"), getProvider().createString("b")));

		assertThat(getProvider().getString(getProvider().getArrayElement(arr, 0))).isEqualTo("a");
		assertThat(getProvider().getString(getProvider().getArrayElement(arr, 1))).isEqualTo("b");
		assertThatThrownBy(() -> getProvider().getArrayElement(arr, -1)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatThrownBy(() -> getProvider().getArrayElement(arr, 2)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	default void testGetArrayElementRejectsNonArrays() {
		List<T> nonArrays = Arrays.asList(
				getProvider().createObject(Collections.emptyMap()),
				getProvider().createString("value"),
				getProvider().createNumber(1),
				getProvider().createBoolean(true),
				getProvider().createNull());

		for (T node : nonArrays)
			assertThatThrownBy(() -> getProvider().getArrayElement(node, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetArrayElementsRejectsNonArrays() {
		assertThatThrownBy(() -> getProvider().getArrayElements(getProvider().createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getArrayElements(getProvider().createString("value"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getArrayElements(getProvider().createNumber(1))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getArrayElements(getProvider().createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getArrayElements(getProvider().createNull())).isInstanceOf(IllegalArgumentException.class);
	}

	// ===================
	// Serialization Tests
	// ===================

	@Test
	default void testFormat() {
		T obj = getProvider().createObject(mapOf("name", getProvider().createString("test"), "value", getProvider().createNumber(42)));

		String json = getProvider().format(obj);
		assertThat(json).contains("\"name\"");
		assertThat(json).contains("\"test\"");
		assertThat(json).contains("\"value\"");
		assertThat(json).contains("42");
	}

	@Test
	default void testFromString() {
		T node = getProvider().parse("{\"foo\": 123, \"bar\": true}");

		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(getProvider().getNumberAsIntExact(getProvider().getObjectMemberOrThrow(node, "foo"))).isEqualTo(123);
		assertThat(getProvider().getBoolean(getProvider().getObjectMemberOrThrow(node, "bar"))).isTrue();
	}

	@Test
	default void testFromStringArray() {
		T node = getProvider().parse("[1, 2, 3]");

		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(getProvider().getArrayLength(node)).isEqualTo(3);
	}

	@Test
	default void testFromStringPrimitives() {
		assertThat(getProvider().getNodeType(getProvider().parse("null"))).isEqualTo(JsonNodeType.NULL);
		assertThat(getProvider().getNodeType(getProvider().parse("true"))).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(getProvider().getNodeType(getProvider().parse("123"))).isEqualTo(JsonNodeType.NUMBER);
		assertThat(getProvider().getNodeType(getProvider().parse("\"hello\""))).isEqualTo(JsonNodeType.STRING);
	}

	// ===================
	// Deep Copy Tests
	// ===================

	@Test
	default void testDeepCopy() {
		T nested = getProvider().createObject(Collections.singletonMap("value", getProvider().createNumber(42)));
		T original = getProvider().createObject(Collections.singletonMap("nested", nested));

		T copy = getProvider().deepCopy(original);

		assertThat(getProvider().format(copy)).isEqualTo(getProvider().format(original));
		assertThat(getProvider().getNumberAsIntExact(getProvider().getObjectMemberOrThrow(getProvider().getObjectMemberOrThrow(copy, "nested"), "value"))).isEqualTo(42);
	}

	// ===================
	// isJsonNodeInstance Tests
	// ===================

	@Test
	default void testIsJsonNodeInstance() {
		T node = getProvider().createNull();
		assertThat(getProvider().isJsonNodeInstance(node)).isTrue();
		assertThat(getProvider().isJsonNodeInstance("not a node")).isFalse();
		assertThat(getProvider().isJsonNodeInstance(42)).isFalse();
		assertThat(getProvider().isJsonNodeInstance(null)).isFalse();
	}

	// ===================
	// Edge Cases
	// ===================

	@Test
	default void testNegativeNumbers() {
		T negInt = getProvider().createNumber(-42);
		assertThat(getProvider().getNumberAsIntExact(negInt)).isEqualTo(-42);

		T negLong = getProvider().createNumber(-9999999999L);
		assertThat(getProvider().getNumberAsLongExact(negLong)).isEqualTo(-9999999999L);

		T negDouble = getProvider().createNumber(-3.14);
		assertThat(getProvider().getNumberAsDoubleRounded(negDouble)).isEqualTo(-3.14);
	}

	@Test
	default void testSpecialStrings() {
		// Test string with special characters
		T node = getProvider().createString("hello\nworld\ttab\"quote");
		assertThat(getProvider().getString(node)).isEqualTo("hello\nworld\ttab\"quote");
	}

	@Test
	default void testUnicodeStrings() {
		T node = getProvider().createString("日本語 emoji: \uD83D\uDE00");
		assertThat(getProvider().getString(node)).isEqualTo("日本語 emoji: \uD83D\uDE00");
	}

	@Test
	default void testNestedStructures() {
		// Create nested object: {"outer": {"inner": [1, 2, 3]}}
		T inner = getProvider().createArray(Arrays.asList(getProvider().createNumber(1), getProvider().createNumber(2), getProvider().createNumber(3)));
		T nested = getProvider().createObject(Collections.singletonMap("inner", inner));
		T outer = getProvider().createObject(Collections.singletonMap("outer", nested));

		// Verify structure
		T retrievedNested = getProvider().getObjectMemberOrThrow(outer, "outer");
		T retrievedArray = getProvider().getObjectMemberOrThrow(retrievedNested, "inner");
		assertThat(getProvider().getArrayLength(retrievedArray)).isEqualTo(3);
		assertThat(getProvider().getNumberAsIntExact(getProvider().getArrayElement(retrievedArray, 1))).isEqualTo(2);
	}

	// ================================
	// Special Number Handling Tests
	// ================================

	// ================================
	// Serialization of Special Values
	// ================================

	@Test
	default void testFormatOnNaN() {
		// format on NaN should return "null" (jq behavior)
		T node = getProvider().createNumber(Double.NaN);
		String json = getProvider().format(node);
		assertThat(json).isEqualTo("null");
	}

	@Test
	default void testFormatOnPositiveInfinity() {
		// format on positive infinity should return the max double value
		T node = getProvider().createNumber(Double.POSITIVE_INFINITY);
		String json = getProvider().format(node);
		assertThat(json).contains("1.7976931348623157e+308");
	}

	@Test
	default void testFormatOnNegativeInfinity() {
		// format on negative infinity should return the negative max double value
		T node = getProvider().createNumber(Double.NEGATIVE_INFINITY);
		String json = getProvider().format(node);
		assertThat(json).contains("-1.7976931348623157e+308");
	}

	@Test
	default void testFormatOnWholeNumberDouble() {
		// format on a whole number double like 0.0 should serialize without decimal (jq behavior)
		T node = getProvider().createNumber(0.0);
		String json = getProvider().format(node);
		assertThat(json).isEqualTo("0");
	}

	@Test
	default void testFormatOnNegativeZero() {
		// format on -0.0 should serialize as "0" (jq behavior)
		T node = getProvider().createNumber(-0.0);
		String json = getProvider().format(node);
		assertThat(json).isEqualTo("0");
	}

	// ================================
	// parse Tests
	// ================================

	@Test
	default void testParseWithEmptyString() {
		// parse on empty string should throw exception
		assertThatThrownBy(() -> getProvider().parse(""))
				.isInstanceOf(JsonException.class);
	}

	@Test
	default void testParseWithTrailingContent() {
		// parse with trailing content should throw exception
		assertThatThrownBy(() -> getProvider().parse("123 456"))
				.isInstanceOf(JsonException.class);
	}

	@Test
	default void testParseWithWhitespaceOnly() {
		// parse on whitespace-only string should throw exception
		assertThatThrownBy(() -> getProvider().parse("   "))
				.isInstanceOf(JsonException.class);
	}

	@Test
	default void testParseWithValidJson() {
		// parse with valid JSON should work
		T node = getProvider().parse("{\"key\": \"value\"}");
		assertThat(getProvider().getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(getProvider().getString(getProvider().getObjectMemberOrThrow(node, "key"))).isEqualTo("value");
	}

	// ================================
	// createParser Tests
	// ================================

	default List<String> parseStream(String json) {
		List<String> result = new ArrayList<>();
		try (JsonParser<T> parser = getProvider().createParser(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			for (T value = parser.next(); value != null; value = parser.next())
				result.add(getProvider().format(value));
		}
		return result;
	}

	@Test
	default void testCreateParserReadsSequenceOfValues() {
		assertThat(parseStream("1 2 {\"a\":3} [4,5] \"six\" null true"))
				.containsExactly("1", "2", "{\"a\":3}", "[4,5]", "\"six\"", "null", "true");
	}

	@Test
	default void testCreateParserReadsValuesWithoutSeparatingWhitespace() {
		assertThat(parseStream("{\"a\":1}{\"b\":2}[1][2]"))
				.containsExactly("{\"a\":1}", "{\"b\":2}", "[1]", "[2]");
	}

	@Test
	default void testCreateParserReadsValuesSeparatedByNewlines() {
		assertThat(parseStream("{\"a\":1}\n{\"b\":2}\n")).containsExactly("{\"a\":1}", "{\"b\":2}");
	}

	@Test
	default void testCreateParserReadsSingleValue() {
		assertThat(parseStream("  {\"k\":\"v\"}  ")).containsExactly("{\"k\":\"v\"}");
	}

	@Test
	default void testCreateParserOnEmptyInput() {
		assertThat(parseStream("")).isEmpty();
	}

	@Test
	default void testCreateParserOnWhitespaceOnlyInput() {
		assertThat(parseStream("  \n\t\r ")).isEmpty();
	}

	@Test
	default void testCreateParserPreservesUnicode() {
		assertThat(parseStream("\"日本語\" \"😀\"")).containsExactly("\"日本語\"", "\"😀\"");
	}

	@Test
	default void testCreateParserKeepsReturningNullAfterExhaustion() {
		try (JsonParser<T> parser = getProvider().createParser(new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)))) {
			assertThat(parser.next()).isNotNull();
			assertThat(parser.next()).isNull();
			assertThat(parser.next()).isNull();
		}
	}

	@Test
	default void testCreateParserReadsValuesLargerThanInternalBuffers() {
		StringBuilder array = new StringBuilder("[");
		for (int i = 0; i < 20000; ++i) {
			if (i > 0)
				array.append(',');
			array.append(i);
		}
		array.append(']');

		assertThat(parseStream(array + " " + array + " 7"))
				.containsExactly(array.toString(), array.toString(), "7");
	}

	// Bare words such as "tru" are deliberately not covered: Gson reads them as strings, and cannot be
	// made to reject them here. Its JsonReader.doPeek() gates both "content follows the first document"
	// and "unquoted literal" on the same checkLenient(), within a single peek(), so the strictness that
	// would reject a bare word also rejects the second document in a sequence.
	@Test
	default void testCreateParserOnMalformedInput() {
		assertThatThrownBy(() -> parseStream("{,,,")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("[1,2")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("{\"a\":}")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("{\"a\":1")).isInstanceOf(JsonException.class);
	}

	@Test
	default void testCreateParserRejectsMalformedValueAfterValidOnes() {
		assertThatThrownBy(() -> parseStream("1 2 }")).isInstanceOf(JsonException.class);
	}

	// ================================
	// HTML Character Escaping Tests
	// ================================

	@Test
	default void testFormatDoesNotEscapeHtmlCharacters() {
		// format() should not escape HTML-like characters (<, >, &, ')
		// This is important for jq @json format compatibility
		T node = getProvider().createString("<>&'\"");
		String json = getProvider().format(node);
		// The string should be JSON-escaped for quotes and backslashes,
		// but HTML characters should NOT be Unicode-escaped
		assertThat(json).isEqualTo("\"<>&'\\\"\"");
	}

	@Test
	default void testFormatObjectWithHtmlCharacters() {
		// Verify HTML characters in object values are not escaped
		T obj = getProvider().createObject(Collections.singletonMap("html", getProvider().createString("<tag>")));
		String json = getProvider().format(obj);
		assertThat(json).contains("\"<tag>\"");
		assertThat(json).doesNotContain("\\u003c"); // Should not Unicode-escape <
	}

	// ================================
	// getNumberType Tests
	// ================================

	@Test
	default void testGetNumberTypeOnNonNumberThrows() {
		assertThatThrownBy(() -> getProvider().getNumberType(getProvider().createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberType(getProvider().createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberType(getProvider().createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberType(getProvider().createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> getProvider().getNumberType(getProvider().createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	default void testGetNumberTypeIsConsistentWithTheAccessors() {
		// Whatever a provider reports, the matching accessor has to work on that node.
		List<T> numbers = Arrays.asList(
				getProvider().createNumber(42),
				getProvider().createNumber(9999999999L),
				getProvider().createNumber(3.14f),
				getProvider().createNumber(3.14159),
				getProvider().createNumber(new BigInteger("123456789012345678901234567890")),
				getProvider().createNumber(new BigDecimal("1.5")),
				getProvider().parse("1"),
				getProvider().parse("1e10"));
		for (T number : numbers) {
			NumberType type = getProvider().getNumberType(number);
			assertThat(type).isNotNull();
			switch (type) {
				case INT:
					assertThat(getProvider().getNumberAsIntExact(number)).isEqualTo(Objects.requireNonNull(getProvider().getNumberAsBigDecimalExact(number)).intValueExact());
					break;
				case LONG:
					assertThat(getProvider().getNumberAsLongExact(number)).isEqualTo(Objects.requireNonNull(getProvider().getNumberAsBigDecimalExact(number)).longValueExact());
					break;
				case BIG_INTEGER:
					assertThat(getProvider().getNumberAsBigIntegerExact(number)).isEqualTo(Objects.requireNonNull(getProvider().getNumberAsBigDecimalExact(number)).toBigIntegerExact());
					break;
				case BIG_DECIMAL:
					assertThat(getProvider().getNumberAsBigDecimalExact(number)).isNotNull();
					break;
				default:
					// DOUBLE, FLOAT and UNKNOWN promise nothing beyond being numbers.
					assertThat(getProvider().getNodeType(number)).isEqualTo(JsonNodeType.NUMBER);
					break;
			}
		}
	}
}
