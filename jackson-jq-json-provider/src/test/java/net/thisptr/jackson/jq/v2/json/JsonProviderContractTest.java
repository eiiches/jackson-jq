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
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test for {@link JsonProvider} implementations.
 * Each provider implementation should extend this class and implement {@link #createProvider()}.
 *
 * @param <T> The JSON node type used by the provider
 */
public abstract class JsonProviderContractTest<T> {

	protected JsonProvider<T> provider;

	/**
	 * Create the JsonProvider instance to test.
	 */
	protected abstract JsonProvider<T> createProvider();

	private T requireGet(T node, String fieldName) {
		return Objects.requireNonNull(provider.get(node, fieldName));
	}

	private T requireGet(T node, int index) {
		return Objects.requireNonNull(provider.get(node, index));
	}

	private static <T> Map<String, T> mapOf(String k1, T v1, String k2, T v2) {
		Map<String, T> map = new LinkedHashMap<>();
		map.put(k1, v1);
		map.put(k2, v2);
		return map;
	}

	private static <T> Map<String, T> mapOf(String k1, T v1, String k2, T v2, String k3, T v3) {
		Map<String, T> map = new LinkedHashMap<>();
		map.put(k1, v1);
		map.put(k2, v2);
		map.put(k3, v3);
		return map;
	}

	@BeforeEach
	void setUp() {
		provider = createProvider();
	}

	// ===================
	// Node Creation Tests
	// ===================

	@Test
	void testCreateNull() {
		T node = provider.createNull();
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NULL);
	}

	@Test
	void testCreateBoolean() {
		T trueNode = provider.createBoolean(true);
		assertThat(provider.getNodeType(trueNode)).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(provider.asBoolean(trueNode)).isTrue();

		T falseNode = provider.createBoolean(false);
		assertThat(provider.getNodeType(falseNode)).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(provider.asBoolean(falseNode)).isFalse();
	}

	@Test
	void testCreateNumberFromInt() {
		T node = provider.createNumber(42);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asInt(node)).isEqualTo(42);
		assertThat(provider.asLong(node)).isEqualTo(42L);
		assertThat(provider.asDoubleRounded(node)).isEqualTo(42.0);
	}

	@Test
	void testCreateNumberFromLong() {
		T node = provider.createNumber(9999999999L);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asLong(node)).isEqualTo(9999999999L);
		assertThat(provider.asDoubleRounded(node)).isEqualTo(9999999999.0);
	}

	@Test
	void testCreateNumberFromFloat() {
		T node = provider.createNumber(3.14f);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat((float) provider.asDoubleRounded(node)).isEqualTo(3.14f);
	}

	@Test
	void testCreateNumberFromDouble() {
		T node = provider.createNumber(3.14159);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDoubleRounded(node)).isEqualTo(3.14159);
	}

	@Test
	void testCreateNumberFromBigInteger() {
		BigInteger value = new BigInteger("123456789012345678901234567890");
		T node = provider.createNumber(value);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDoubleRounded(node)).isEqualTo(value.doubleValue());
	}

	@Test
	void testCreateNumberFromBigDecimal() {
		BigDecimal value = new BigDecimal("3.14159265358979323846264338327950288");
		T node = provider.createNumber(value);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDoubleRounded(node)).isEqualTo(value.doubleValue());
	}

	@Test
	void testAsBigDecimalIsLossless() {
		assertThat(provider.asBigDecimal(provider.createNumber(42))).isEqualByComparingTo("42");

		// The whole point: this value is not representable as a double.
		assertThat(provider.asBigDecimal(provider.createNumber(2871948651097801136L))).isEqualByComparingTo("2871948651097801136");

		BigInteger bigInteger = new BigInteger("123456789012345678901234567890");
		assertThat(provider.asBigDecimal(provider.createNumber(bigInteger))).isEqualByComparingTo(new BigDecimal(bigInteger));

		BigDecimal bigDecimal = new BigDecimal("3.14159265358979323846264338327950288");
		assertThat(provider.asBigDecimal(provider.createNumber(bigDecimal))).isEqualByComparingTo(bigDecimal);
	}

	@Test
	void testAsBigDecimalOnDoubleUsesShortestRepresentation() {
		// Not the exact binary expansion (0.1000000000000000055511151231257827...), which would stop
		// a computed 0.1 from comparing equal to the literal 0.1.
		assertThat(provider.asBigDecimal(provider.createNumber(0.1))).isEqualByComparingTo("0.1");
		assertThat(provider.asBigDecimal(provider.createNumber(-3.14))).isEqualByComparingTo("-3.14");
	}

	@Test
	void testAsBigDecimalOnNonFiniteReturnsNull() {
		assertThat(provider.asBigDecimal(provider.createNumber(Double.NaN))).isNull();
		assertThat(provider.asBigDecimal(provider.createNumber(Double.POSITIVE_INFINITY))).isNull();
		assertThat(provider.asBigDecimal(provider.createNumber(Double.NEGATIVE_INFINITY))).isNull();
	}

	@Test
	void testAsBigDecimalOnNonNumberThrows() {
		assertThatThrownBy(() -> provider.asBigDecimal(provider.createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asBigDecimal(provider.createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asBigDecimal(provider.createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asBigDecimal(provider.createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asBigDecimal(provider.createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testExactIntegralAccessorsReturnNullForFractionalValues() {
		T positive = provider.createNumber(1.9);
		T negative = provider.createNumber(-1.9);

		assertThat(provider.asBigInteger(positive)).isNull();
		assertThat(provider.asBigInteger(negative)).isNull();
		assertThat(provider.asInt(positive)).isNull();
		assertThat(provider.asInt(negative)).isNull();
		assertThat(provider.asLong(positive)).isNull();
		assertThat(provider.asLong(negative)).isNull();
	}

	@Test
	void testExactIntegralAccessorsReturnNullOutOfRange() {
		assertThat(provider.asInt(provider.createNumber(2147483648L))).isNull();
		assertThat(provider.asInt(provider.createNumber(-2147483649L))).isNull();
		// Out of range because of the value, whatever the representation holding it.
		assertThat(provider.asInt(provider.createNumber(1e15))).isNull();
		assertThat(provider.asInt(provider.createNumber(-1e15))).isNull();
		assertThat(provider.asLong(provider.createNumber(new BigInteger("123456789012345678901234567890")))).isNull();
	}

	@Test
	void testIntegralAccessorsReturnNullForNonFiniteValues() {
		for (double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
			T node = provider.createNumber(value);
			assertThat(provider.asBigInteger(node)).isNull();
			assertThat(provider.asBigIntegerTruncated(node)).isNull();
			assertThat(provider.asInt(node)).isNull();
			assertThat(provider.asLong(node)).isNull();
			assertThat(provider.asIntTruncated(node)).isNull();
			assertThat(provider.asLongTruncated(node)).isNull();
		}
	}

	@Test
	void testIntegralAccessorsThrowOnNonNumber() {
		List<T> nonNumbers = Arrays.asList(
				provider.createString("42"),
				provider.createBoolean(true),
				provider.createNull(),
				provider.createArray(Collections.emptyList()),
				provider.createObject(Collections.emptyMap()));
		for (T node : nonNumbers) {
			assertThatThrownBy(() -> provider.asBigInteger(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> provider.asBigIntegerTruncated(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> provider.asInt(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> provider.asLong(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> provider.asIntTruncated(node)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> provider.asLongTruncated(node)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void testAsDoubleRoundedThrowsOnNonNumber() {
		// A NaN return therefore means the value is NaN, never that the node was the wrong type.
		assertThatThrownBy(() -> provider.asDoubleRounded(provider.createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asDoubleRounded(provider.createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asDoubleRounded(provider.createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asDoubleRounded(provider.createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asDoubleRounded(provider.createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testAsDoubleRoundedRoundsToNearest() {
		// Rounding, not truncation: the example documented by JsonProvider lands above the exact value.
		assertThat(provider.asDoubleRounded(provider.createNumber(2871948651097801136L))).isEqualTo(0x1.3ed9b0a7cec61p61);
		// These are exact halfway cases on opposite sides of an even significand.
		assertThat(provider.asDoubleRounded(provider.createNumber(9007199254740993L))).isEqualTo(0x1.0p53);
		assertThat(provider.asDoubleRounded(provider.createNumber(9007199254740995L))).isEqualTo(0x1.0000000000002p53);
	}

	@Test
	void testAsDoubleRoundedHandlesNonFiniteResults() {
		assertThat(provider.asDoubleRounded(provider.createNumber(new BigDecimal("1e400")))).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(provider.asDoubleRounded(provider.createNumber(new BigDecimal("-1e400")))).isEqualTo(Double.NEGATIVE_INFINITY);
		assertThat(provider.asDoubleRounded(provider.createNumber(Double.POSITIVE_INFINITY))).isEqualTo(Double.POSITIVE_INFINITY);
		assertThat(provider.asDoubleRounded(provider.createNumber(Double.NEGATIVE_INFINITY))).isEqualTo(Double.NEGATIVE_INFINITY);
		assertThat(provider.asDoubleRounded(provider.createNumber(Double.NaN))).isNaN();
	}

	@Test
	void testTruncatedIntegralAccessorsRoundTowardZero() {
		T positive = provider.createNumber(1.9);
		T negative = provider.createNumber(-1.9);

		assertThat(provider.asBigIntegerTruncated(positive)).isEqualTo(BigInteger.ONE);
		assertThat(provider.asBigIntegerTruncated(negative)).isEqualTo(BigInteger.ONE.negate());
		assertThat(provider.asIntTruncated(positive)).isEqualTo(1);
		assertThat(provider.asIntTruncated(negative)).isEqualTo(-1);
		assertThat(provider.asLongTruncated(positive)).isEqualTo(1L);
		assertThat(provider.asLongTruncated(negative)).isEqualTo(-1L);
	}

	@Test
	void testBigIntegerConversionsHaveNoRangeLimit() {
		BigInteger hugeInteger = BigInteger.TEN.pow(400);
		assertThat(provider.asBigInteger(provider.createNumber(hugeInteger))).isEqualTo(hugeInteger);
		assertThat(provider.asBigInteger(provider.createNumber(new BigDecimal("1.000e400")))).isEqualTo(hugeInteger);
		assertThat(provider.asBigInteger(provider.createNumber(1e100))).isEqualTo(BigInteger.TEN.pow(100));
		assertThat(provider.asBigInteger(provider.createNumber(42.0f))).isEqualTo(BigInteger.valueOf(42));

		BigDecimal hugeFraction = new BigDecimal(hugeInteger).add(new BigDecimal("0.9"));
		assertThat(provider.asBigIntegerTruncated(provider.createNumber(hugeFraction))).isEqualTo(hugeInteger);
		assertThat(provider.asBigIntegerTruncated(provider.createNumber(hugeFraction.negate()))).isEqualTo(hugeInteger.negate());
		assertThat(provider.asBigIntegerTruncated(provider.createNumber(0.9))).isEqualTo(BigInteger.ZERO);
		assertThat(provider.asBigIntegerTruncated(provider.createNumber(-0.9))).isEqualTo(BigInteger.ZERO);
	}

	@Test
	void testTruncatedIntChecksRangeAfterTruncation() {
		assertThat(provider.asIntTruncated(provider.createNumber(2147483647.9))).isEqualTo(Integer.MAX_VALUE);
		assertThat(provider.asIntTruncated(provider.createNumber(-2147483648.9))).isEqualTo(Integer.MIN_VALUE);
		assertThat(provider.asIntTruncated(provider.createNumber(2147483648.0))).isNull();
		assertThat(provider.asIntTruncated(provider.createNumber(-2147483649.0))).isNull();
	}

	@Test
	void testTruncatedLongDoesNotLoseDoublePrecision() {
		long value = 9007199254740993L;
		assertThat(provider.asLongTruncated(provider.createNumber(value))).isEqualTo(value);
	}

	@Test
	void testLongConversionsBeyondExactDoubleIntegerBoundary() {
		double above = Math.nextUp(0x1p53);
		double below = Math.nextDown(-0x1p53);

		assertThat(provider.asLong(provider.createNumber(above))).isEqualTo(9_007_199_254_740_994L);
		assertThat(provider.asLongTruncated(provider.createNumber(above))).isEqualTo(9_007_199_254_740_994L);
		assertThat(provider.asLong(provider.createNumber(below))).isEqualTo(-9_007_199_254_740_994L);
		assertThat(provider.asLongTruncated(provider.createNumber(below))).isEqualTo(-9_007_199_254_740_994L);

		long positiveOdd = 9_007_199_254_740_995L;
		long negativeOdd = -9_007_199_254_740_995L;
		assertThat(provider.asLong(provider.createNumber((double) positiveOdd))).isEqualTo(9_007_199_254_740_996L);
		assertThat(provider.asLong(provider.createNumber((double) negativeOdd))).isEqualTo(-9_007_199_254_740_996L);
		assertThat(provider.asLong(provider.createNumber(positiveOdd))).isEqualTo(positiveOdd);
		assertThat(provider.asLong(provider.createNumber(negativeOdd))).isEqualTo(negativeOdd);

		double largestLong = Math.nextDown(0x1p63);
		assertThat(provider.asLong(provider.createNumber(largestLong))).isEqualTo((long) largestLong);
		assertThat(provider.asLong(provider.createNumber(0x1p63))).isNull();
		assertThat(provider.asLongTruncated(provider.createNumber(0x1p63))).isNull();
	}

	@Test
	void testTruncatedIntegralAccessorsReturnNullForUnrepresentableValues() {
		// A wrong node type still throws; only "no representative exists" is null.
		T text = provider.createString("1");
		assertThatThrownBy(() -> provider.asIntTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLongTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThat(provider.asIntTruncated(provider.createNumber(Double.NaN))).isNull();
		assertThat(provider.asLongTruncated(provider.createNumber(Double.POSITIVE_INFINITY))).isNull();
		assertThat(provider.asLongTruncated(provider.createNumber(1e20))).isNull();
	}

	@Test
	void testCreateString() {
		T node = provider.createString("hello");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(provider.asString(node)).isEqualTo("hello");
	}

	@Test
	void testCreateEmptyString() {
		T node = provider.createString("");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(provider.asString(node)).isEqualTo("");
	}

	@Test
	void testCreateObject() {
		T node = provider.createObject(Collections.emptyMap());
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.size(node)).isEqualTo(0);
	}

	@Test
	void testCreateArray() {
		T node = provider.createArray(Collections.emptyList());
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(provider.size(node)).isEqualTo(0);
	}

	@Test
	void testCreateArrayFromValues() {
		T node = provider.createArray(Arrays.asList(provider.createNumber(1), provider.createString("two")));

		assertThat(provider.size(node)).isEqualTo(2);
		assertThat(provider.asInt(requireGet(node, 0))).isEqualTo(1);
		assertThat(provider.asString(requireGet(node, 1))).isEqualTo("two");
	}

	@Test
	void testCreateObjectFromValues() {
		Map<String, T> values = new LinkedHashMap<>();
		values.put("one", provider.createNumber(1));
		values.put("two", provider.createString("two"));
		T node = provider.createObject(values);

		assertThat(provider.size(node)).isEqualTo(2);
		assertThat(provider.asInt(requireGet(node, "one"))).isEqualTo(1);
		assertThat(provider.asString(requireGet(node, "two"))).isEqualTo("two");
	}

	// ===================
	// Object Operations
	// ===================

	@Test
	void testObjectFields() {
		T obj = provider.createObject(mapOf("x", provider.createNumber(10), "y", provider.createNumber(20)));

		List<String> keys = new ArrayList<>();
		List<Integer> values = new ArrayList<>();
		Iterator<Map.Entry<String, T>> it = provider.fields(obj);
		while (it.hasNext()) {
			Map.Entry<String, T> entry = it.next();
			keys.add(entry.getKey());
			values.add(provider.asInt(entry.getValue()));
		}

		assertThat(keys).containsExactlyInAnyOrder("x", "y");
		assertThat(values).containsExactlyInAnyOrder(10, 20);
	}

	@Test
	void testObjectFieldNames() {
		T obj = provider.createObject(mapOf("foo", provider.createNull(), "bar", provider.createNull()));

		List<String> names = new ArrayList<>();
		Iterator<String> it = provider.fieldNames(obj);
		while (it.hasNext()) {
			names.add(it.next());
		}

		assertThat(names).containsExactlyInAnyOrder("foo", "bar");
	}

	// ===================
	// Array Operations
	// ===================

	@Test
	void testArrayElements() {
		T arr = provider.createArray(Arrays.asList(provider.createString("a"), provider.createString("b"), provider.createString("c")));

		List<String> elements = new ArrayList<>();
		Iterator<T> it = provider.elements(arr);
		while (it.hasNext()) {
			elements.add(provider.asString(it.next()));
		}

		assertThat(elements).containsExactly("a", "b", "c");
	}

	// ===================
	// Serialization Tests
	// ===================

	@Test
	void testFormat() {
		T obj = provider.createObject(mapOf("name", provider.createString("test"), "value", provider.createNumber(42)));

		String json = provider.format(obj);
		assertThat(json).contains("\"name\"");
		assertThat(json).contains("\"test\"");
		assertThat(json).contains("\"value\"");
		assertThat(json).contains("42");
	}

	@Test
	void testFromString() {
		T node = provider.parse("{\"foo\": 123, \"bar\": true}");

		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.asInt(requireGet(node, "foo"))).isEqualTo(123);
		assertThat(provider.asBoolean(requireGet(node, "bar"))).isTrue();
	}

	@Test
	void testFromStringArray() {
		T node = provider.parse("[1, 2, 3]");

		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(provider.size(node)).isEqualTo(3);
	}

	@Test
	void testFromStringPrimitives() {
		assertThat(provider.getNodeType(provider.parse("null"))).isEqualTo(JsonNodeType.NULL);
		assertThat(provider.getNodeType(provider.parse("true"))).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(provider.getNodeType(provider.parse("123"))).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.getNodeType(provider.parse("\"hello\""))).isEqualTo(JsonNodeType.STRING);
	}

	// ===================
	// Deep Copy Tests
	// ===================

	@Test
	void testDeepCopy() {
		T nested = provider.createObject(Collections.singletonMap("value", provider.createNumber(42)));
		T original = provider.createObject(Collections.singletonMap("nested", nested));

		T copy = provider.deepCopy(original);

		assertThat(provider.format(copy)).isEqualTo(provider.format(original));
		assertThat(provider.asInt(requireGet(requireGet(copy, "nested"), "value"))).isEqualTo(42);
	}

	// ===================
	// isJsonNodeInstance Tests
	// ===================

	@Test
	void testIsJsonNodeInstance() {
		T node = provider.createNull();
		assertThat(provider.isJsonNodeInstance(node)).isTrue();
		assertThat(provider.isJsonNodeInstance("not a node")).isFalse();
		assertThat(provider.isJsonNodeInstance(42)).isFalse();
		assertThat(provider.isJsonNodeInstance(null)).isFalse();
	}

	// ===================
	// Edge Cases
	// ===================

	@Test
	void testNegativeNumbers() {
		T negInt = provider.createNumber(-42);
		assertThat(provider.asInt(negInt)).isEqualTo(-42);

		T negLong = provider.createNumber(-9999999999L);
		assertThat(provider.asLong(negLong)).isEqualTo(-9999999999L);

		T negDouble = provider.createNumber(-3.14);
		assertThat(provider.asDoubleRounded(negDouble)).isEqualTo(-3.14);
	}

	@Test
	void testSpecialStrings() {
		// Test string with special characters
		T node = provider.createString("hello\nworld\ttab\"quote");
		assertThat(provider.asString(node)).isEqualTo("hello\nworld\ttab\"quote");
	}

	@Test
	void testUnicodeStrings() {
		T node = provider.createString("日本語 emoji: \uD83D\uDE00");
		assertThat(provider.asString(node)).isEqualTo("日本語 emoji: \uD83D\uDE00");
	}

	@Test
	void testNestedStructures() {
		// Create nested object: {"outer": {"inner": [1, 2, 3]}}
		T inner = provider.createArray(Arrays.asList(provider.createNumber(1), provider.createNumber(2), provider.createNumber(3)));
		T nested = provider.createObject(Collections.singletonMap("inner", inner));
		T outer = provider.createObject(Collections.singletonMap("outer", nested));

		// Verify structure
		T retrievedNested = requireGet(outer, "outer");
		T retrievedArray = requireGet(retrievedNested, "inner");
		assertThat(provider.size(retrievedArray)).isEqualTo(3);
		assertThat(provider.asInt(requireGet(retrievedArray, 1))).isEqualTo(2);
	}

	// ================================
	// Special Number Handling Tests
	// ================================

	@Test
	void testAsStringOnNullNode() {
		// asText on null node should return "null", not empty string
		T node = provider.createNull();
		assertThat(provider.asString(node)).isEqualTo("null");
	}

	// ================================
	// Serialization of Special Values
	// ================================

	@Test
	void testFormatOnNaN() {
		// toString on NaN should return "null" (jq behavior)
		T node = provider.createNumber(Double.NaN);
		String json = provider.format(node);
		assertThat(json).isEqualTo("null");
	}

	@Test
	void testFormatOnPositiveInfinity() {
		// toString on positive infinity should return the max double value
		T node = provider.createNumber(Double.POSITIVE_INFINITY);
		String json = provider.format(node);
		assertThat(json).contains("1.7976931348623157e+308");
	}

	@Test
	void testFormatOnNegativeInfinity() {
		// toString on negative infinity should return the negative max double value
		T node = provider.createNumber(Double.NEGATIVE_INFINITY);
		String json = provider.format(node);
		assertThat(json).contains("-1.7976931348623157e+308");
	}

	@Test
	void testFormatOnWholeNumberDouble() {
		// toString on a whole number double like 0.0 should serialize without decimal (jq behavior)
		T node = provider.createNumber(0.0);
		String json = provider.format(node);
		assertThat(json).isEqualTo("0");
	}

	@Test
	void testFormatOnNegativeZero() {
		// toString on -0.0 should serialize as "0" (jq behavior)
		T node = provider.createNumber(-0.0);
		String json = provider.format(node);
		assertThat(json).isEqualTo("0");
	}

	// ================================
	// parse Tests
	// ================================

	@Test
	void testParseWithEmptyString() {
		// parse on empty string should throw exception
		assertThatThrownBy(() -> provider.parse(""))
				.isInstanceOf(JsonException.class);
	}

	@Test
	void testParseWithTrailingContent() {
		// parse with trailing content should throw exception
		assertThatThrownBy(() -> provider.parse("123 456"))
				.isInstanceOf(JsonException.class);
	}

	@Test
	void testParseWithWhitespaceOnly() {
		// parse on whitespace-only string should throw exception
		assertThatThrownBy(() -> provider.parse("   "))
				.isInstanceOf(JsonException.class);
	}

	@Test
	void testParseWithValidJson() {
		// parse with valid JSON should work
		T node = provider.parse("{\"key\": \"value\"}");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.asString(requireGet(node, "key"))).isEqualTo("value");
	}

	// ================================
	// createParser Tests
	// ================================

	private List<String> parseStream(String json) {
		List<String> result = new ArrayList<>();
		try (JsonParser<T> parser = provider.createParser(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
			for (T value = parser.next(); value != null; value = parser.next())
				result.add(provider.format(value));
		}
		return result;
	}

	@Test
	void testCreateParserReadsSequenceOfValues() {
		assertThat(parseStream("1 2 {\"a\":3} [4,5] \"six\" null true"))
				.containsExactly("1", "2", "{\"a\":3}", "[4,5]", "\"six\"", "null", "true");
	}

	@Test
	void testCreateParserReadsValuesWithoutSeparatingWhitespace() {
		assertThat(parseStream("{\"a\":1}{\"b\":2}[1][2]"))
				.containsExactly("{\"a\":1}", "{\"b\":2}", "[1]", "[2]");
	}

	@Test
	void testCreateParserReadsValuesSeparatedByNewlines() {
		assertThat(parseStream("{\"a\":1}\n{\"b\":2}\n")).containsExactly("{\"a\":1}", "{\"b\":2}");
	}

	@Test
	void testCreateParserReadsSingleValue() {
		assertThat(parseStream("  {\"k\":\"v\"}  ")).containsExactly("{\"k\":\"v\"}");
	}

	@Test
	void testCreateParserOnEmptyInput() {
		assertThat(parseStream("")).isEmpty();
	}

	@Test
	void testCreateParserOnWhitespaceOnlyInput() {
		assertThat(parseStream("  \n\t\r ")).isEmpty();
	}

	@Test
	void testCreateParserPreservesUnicode() {
		assertThat(parseStream("\"日本語\" \"😀\"")).containsExactly("\"日本語\"", "\"😀\"");
	}

	@Test
	void testCreateParserKeepsReturningNullAfterExhaustion() {
		try (JsonParser<T> parser = provider.createParser(new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8)))) {
			assertThat(parser.next()).isNotNull();
			assertThat(parser.next()).isNull();
			assertThat(parser.next()).isNull();
		}
	}

	@Test
	void testCreateParserReadsValuesLargerThanInternalBuffers() {
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
	void testCreateParserOnMalformedInput() {
		assertThatThrownBy(() -> parseStream("{,,,")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("[1,2")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("{\"a\":}")).isInstanceOf(JsonException.class);
		assertThatThrownBy(() -> parseStream("{\"a\":1")).isInstanceOf(JsonException.class);
	}

	@Test
	void testCreateParserRejectsMalformedValueAfterValidOnes() {
		assertThatThrownBy(() -> parseStream("1 2 }")).isInstanceOf(JsonException.class);
	}

	// ================================
	// Object elements() Tests
	// ================================

	@Test
	void testObjectElements() {
		// elements() on an object should return an iterator over the field values
		// This is important for jq functions like from_entries that iterate over object values
		T obj = provider.createObject(mapOf("a", provider.createNumber(1), "b", provider.createNumber(2), "c", provider.createNumber(3)));

		List<Integer> values = new ArrayList<>();
		Iterator<T> it = provider.elements(obj);
		while (it.hasNext()) {
			values.add(provider.asInt(it.next()));
		}

		assertThat(values).containsExactlyInAnyOrder(1, 2, 3);
	}

	// ================================
	// HTML Character Escaping Tests
	// ================================

	@Test
	void testFormatDoesNotEscapeHtmlCharacters() {
		// format() should not escape HTML-like characters (<, >, &, ')
		// This is important for jq @json format compatibility
		T node = provider.createString("<>&'\"");
		String json = provider.format(node);
		// The string should be JSON-escaped for quotes and backslashes,
		// but HTML characters should NOT be Unicode-escaped
		assertThat(json).isEqualTo("\"<>&'\\\"\"");
	}

	@Test
	void testFormatObjectWithHtmlCharacters() {
		// Verify HTML characters in object values are not escaped
		T obj = provider.createObject(Collections.singletonMap("html", provider.createString("<tag>")));
		String json = provider.format(obj);
		assertThat(json).contains("\"<tag>\"");
		assertThat(json).doesNotContain("\\u003c"); // Should not Unicode-escape <
	}

	// ================================
	// getNumberType Tests
	// ================================

	@Test
	void testGetNumberTypeOnNonNumberThrows() {
		assertThatThrownBy(() -> provider.getNumberType(provider.createString("42"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.getNumberType(provider.createBoolean(true))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.getNumberType(provider.createNull())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.getNumberType(provider.createArray(Collections.emptyList()))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.getNumberType(provider.createObject(Collections.emptyMap()))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testGetNumberTypeIsConsistentWithTheAccessors() {
		// Whatever a provider reports, the matching accessor has to work on that node.
		List<T> numbers = Arrays.asList(
				provider.createNumber(42),
				provider.createNumber(9999999999L),
				provider.createNumber(3.14f),
				provider.createNumber(3.14159),
				provider.createNumber(new BigInteger("123456789012345678901234567890")),
				provider.createNumber(new BigDecimal("1.5")),
				provider.parse("1"),
				provider.parse("1e10"));
		for (T number : numbers) {
			NumberType type = provider.getNumberType(number);
			assertThat(type).isNotNull();
			switch (type) {
				case INT:
					assertThat(provider.asInt(number)).isEqualTo(Objects.requireNonNull(provider.asBigDecimal(number)).intValueExact());
					break;
				case LONG:
					assertThat(provider.asLong(number)).isEqualTo(Objects.requireNonNull(provider.asBigDecimal(number)).longValueExact());
					break;
				case BIG_INTEGER:
					assertThat(provider.asBigInteger(number)).isEqualTo(Objects.requireNonNull(provider.asBigDecimal(number)).toBigIntegerExact());
					break;
				case BIG_DECIMAL:
					assertThat(provider.asBigDecimal(number)).isNotNull();
					break;
				default:
					// DOUBLE, FLOAT and UNKNOWN promise nothing beyond being numbers.
					assertThat(provider.getNodeType(number)).isEqualTo(JsonNodeType.NUMBER);
					break;
			}
		}
	}
}
