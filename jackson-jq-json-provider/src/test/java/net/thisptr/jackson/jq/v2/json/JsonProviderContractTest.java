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
		assertThat(provider.asDouble(node)).isEqualTo(42.0);
	}

	@Test
	void testCreateNumberFromLong() {
		T node = provider.createNumber(9999999999L);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asLong(node)).isEqualTo(9999999999L);
		assertThat(provider.asDouble(node)).isEqualTo(9999999999.0);
	}

	@Test
	void testCreateNumberFromFloat() {
		T node = provider.createNumber(3.14f);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat((float) provider.asDouble(node)).isEqualTo(3.14f);
	}

	@Test
	void testCreateNumberFromDouble() {
		T node = provider.createNumber(3.14159);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDouble(node)).isEqualTo(3.14159);
	}

	@Test
	void testCreateNumberFromBigInteger() {
		BigInteger value = new BigInteger("123456789012345678901234567890");
		T node = provider.createNumber(value);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDouble(node)).isEqualTo(value.doubleValue());
	}

	@Test
	void testCreateNumberFromBigDecimal() {
		BigDecimal value = new BigDecimal("3.14159265358979323846264338327950288");
		T node = provider.createNumber(value);
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.asDouble(node)).isEqualTo(value.doubleValue());
	}

	@Test
	void testExactIntegralAccessorsRejectFractionalValues() {
		T positive = provider.createNumber(1.9);
		T negative = provider.createNumber(-1.9);

		assertThatThrownBy(() -> provider.asInt(positive)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asInt(negative)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLong(positive)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLong(negative)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testTruncatedIntegralAccessorsRoundTowardZero() {
		T positive = provider.createNumber(1.9);
		T negative = provider.createNumber(-1.9);

		assertThat(provider.asIntTruncated(positive)).isEqualTo(1);
		assertThat(provider.asIntTruncated(negative)).isEqualTo(-1);
		assertThat(provider.asLongTruncated(positive)).isEqualTo(1L);
		assertThat(provider.asLongTruncated(negative)).isEqualTo(-1L);
	}

	@Test
	void testTruncatedIntChecksRangeAfterTruncation() {
		assertThat(provider.asIntTruncated(provider.createNumber(2147483647.9))).isEqualTo(Integer.MAX_VALUE);
		assertThat(provider.asIntTruncated(provider.createNumber(-2147483648.9))).isEqualTo(Integer.MIN_VALUE);
		assertThatThrownBy(() -> provider.asIntTruncated(provider.createNumber(2147483648.0)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asIntTruncated(provider.createNumber(-2147483649.0)))
				.isInstanceOf(IllegalArgumentException.class);
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
		assertThatThrownBy(() -> provider.asLong(provider.createNumber(0x1p63))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLongTruncated(provider.createNumber(0x1p63)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testTruncatedIntegralAccessorsRejectInvalidValues() {
		T text = provider.createString("1");
		assertThatThrownBy(() -> provider.asIntTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLongTruncated(text)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asIntTruncated(provider.createNumber(Double.NaN))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLongTruncated(provider.createNumber(Double.POSITIVE_INFINITY))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> provider.asLongTruncated(provider.createNumber(1e20))).isInstanceOf(IllegalArgumentException.class);
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
		assertThat(provider.asDouble(negDouble)).isEqualTo(-3.14);
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

	@Test
	void testAsIntOnNaNThrows() {
		// asInt on NaN should throw exception (strict semantics)
		T node = provider.createNumber(Double.NaN);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsLongOnNaNThrows() {
		// asLong on NaN should throw exception (strict semantics)
		T node = provider.createNumber(Double.NaN);
		assertThatThrownBy(() -> provider.asLong(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnPositiveInfinityThrows() {
		// asInt on positive infinity should throw exception (strict semantics)
		T node = provider.createNumber(Double.POSITIVE_INFINITY);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnNegativeInfinityThrows() {
		// asInt on negative infinity should throw exception (strict semantics)
		T node = provider.createNumber(Double.NEGATIVE_INFINITY);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsLongOnPositiveInfinityThrows() {
		// asLong on positive infinity should throw exception (strict semantics)
		T node = provider.createNumber(Double.POSITIVE_INFINITY);
		assertThatThrownBy(() -> provider.asLong(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsLongOnNegativeInfinityThrows() {
		// asLong on negative infinity should throw exception (strict semantics)
		T node = provider.createNumber(Double.NEGATIVE_INFINITY);
		assertThatThrownBy(() -> provider.asLong(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnLargeNumberThrows() {
		// asInt on a number larger than Integer.MAX_VALUE should throw exception (strict semantics)
		T node = provider.createNumber(1_000_000_000_000_000_000L);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnSmallNumberThrows() {
		// asInt on a number smaller than Integer.MIN_VALUE should throw exception (strict semantics)
		T node = provider.createNumber(-1_000_000_000_000_000_000L);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnDoubleLargePositiveThrows() {
		// asInt on a double larger than Integer.MAX_VALUE should throw exception
		T node = provider.createNumber(1e15);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void testAsIntOnDoubleLargeNegativeThrows() {
		// asInt on a double smaller than Integer.MIN_VALUE should throw exception
		T node = provider.createNumber(-1e15);
		assertThatThrownBy(() -> provider.asInt(node))
				.isInstanceOf(RuntimeException.class);
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
}
