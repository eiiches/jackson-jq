package net.thisptr.jackson.jq.v2.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
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
	void testCreateString() {
		T node = provider.createString("hello");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(provider.asText(node)).isEqualTo("hello");
	}

	@Test
	void testCreateEmptyString() {
		T node = provider.createString("");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.STRING);
		assertThat(provider.asText(node)).isEqualTo("");
	}

	@Test
	void testCreateObject() {
		T node = provider.createObject();
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.size(node)).isEqualTo(0);
	}

	@Test
	void testCreateArray() {
		T node = provider.createArray();
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(provider.size(node)).isEqualTo(0);
	}

	// ===================
	// Object Operations
	// ===================

	@Test
	void testObjectSetAndGet() {
		@Var T obj = provider.createObject();
		T value = provider.createString("world");
		obj = provider.set(obj, "hello", value);

		assertThat(provider.has(obj, "hello")).isTrue();
		assertThat(provider.has(obj, "nonexistent")).isFalse();
		assertThat(provider.size(obj)).isEqualTo(1);

		T retrieved = requireGet(obj, "hello");
		assertThat(provider.asText(retrieved)).isEqualTo("world");
	}

	@Test
	void testObjectMultipleFields() {
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "a", provider.createNumber(1));
		obj = provider.set(obj, "b", provider.createNumber(2));
		obj = provider.set(obj, "c", provider.createNumber(3));

		assertThat(provider.size(obj)).isEqualTo(3);
		assertThat(provider.asInt(requireGet(obj, "a"))).isEqualTo(1);
		assertThat(provider.asInt(requireGet(obj, "b"))).isEqualTo(2);
		assertThat(provider.asInt(requireGet(obj, "c"))).isEqualTo(3);
	}

	@Test
	void testObjectFields() {
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "x", provider.createNumber(10));
		obj = provider.set(obj, "y", provider.createNumber(20));

		List<String> keys = new ArrayList<>();
		List<Integer> values = new ArrayList<>();
		Iterator<Entry<String, T>> it = provider.fields(obj);
		while (it.hasNext()) {
			Entry<String, T> entry = it.next();
			keys.add(entry.getKey());
			values.add(provider.asInt(entry.getValue()));
		}

		assertThat(keys).containsExactlyInAnyOrder("x", "y");
		assertThat(values).containsExactlyInAnyOrder(10, 20);
	}

	@Test
	void testObjectFieldNames() {
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "foo", provider.createNull());
		obj = provider.set(obj, "bar", provider.createNull());

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
	void testArrayAddAndGet() {
		@Var T arr = provider.createArray();
		arr = provider.add(arr, provider.createNumber(1));
		arr = provider.add(arr, provider.createNumber(2));
		arr = provider.add(arr, provider.createNumber(3));

		assertThat(provider.size(arr)).isEqualTo(3);
		assertThat(provider.has(arr, 0)).isTrue();
		assertThat(provider.has(arr, 2)).isTrue();
		assertThat(provider.has(arr, 3)).isFalse();

		assertThat(provider.asInt(requireGet(arr, 0))).isEqualTo(1);
		assertThat(provider.asInt(requireGet(arr, 1))).isEqualTo(2);
		assertThat(provider.asInt(requireGet(arr, 2))).isEqualTo(3);
	}

	@Test
	void testArraySet() {
		@Var T arr = provider.createArray();
		arr = provider.add(arr, provider.createNumber(1));
		arr = provider.add(arr, provider.createNumber(2));
		arr = provider.add(arr, provider.createNumber(3));

		arr = provider.set(arr, 1, provider.createNumber(99));

		assertThat(provider.asInt(requireGet(arr, 0))).isEqualTo(1);
		assertThat(provider.asInt(requireGet(arr, 1))).isEqualTo(99);
		assertThat(provider.asInt(requireGet(arr, 2))).isEqualTo(3);
	}

	@Test
	void testArrayElements() {
		@Var T arr = provider.createArray();
		arr = provider.add(arr, provider.createString("a"));
		arr = provider.add(arr, provider.createString("b"));
		arr = provider.add(arr, provider.createString("c"));

		List<String> elements = new ArrayList<>();
		Iterator<T> it = provider.elements(arr);
		while (it.hasNext()) {
			elements.add(provider.asText(it.next()));
		}

		assertThat(elements).containsExactly("a", "b", "c");
	}

	@Test
	void testArrayIterate() {
		@Var T arr = provider.createArray();
		arr = provider.add(arr, provider.createNumber(10));
		arr = provider.add(arr, provider.createNumber(20));

		List<Integer> values = new ArrayList<>();
		for (T element : provider.iterate(arr)) {
			values.add(provider.asInt(element));
		}

		assertThat(values).containsExactly(10, 20);
	}

	// ===================
	// Serialization Tests
	// ===================

	@Test
	void testToString() {
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "name", provider.createString("test"));
		obj = provider.set(obj, "value", provider.createNumber(42));

		String json = provider.toString(obj);
		assertThat(json).contains("\"name\"");
		assertThat(json).contains("\"test\"");
		assertThat(json).contains("\"value\"");
		assertThat(json).contains("42");
	}

	@Test
	void testFromString() throws Exception {
		T node = provider.fromString("{\"foo\": 123, \"bar\": true}");

		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.asInt(requireGet(node, "foo"))).isEqualTo(123);
		assertThat(provider.asBoolean(requireGet(node, "bar"))).isTrue();
	}

	@Test
	void testFromStringArray() throws Exception {
		T node = provider.fromString("[1, 2, 3]");

		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.ARRAY);
		assertThat(provider.size(node)).isEqualTo(3);
	}

	@Test
	void testFromStringPrimitives() throws Exception {
		assertThat(provider.getNodeType(provider.fromString("null"))).isEqualTo(JsonNodeType.NULL);
		assertThat(provider.getNodeType(provider.fromString("true"))).isEqualTo(JsonNodeType.BOOLEAN);
		assertThat(provider.getNodeType(provider.fromString("123"))).isEqualTo(JsonNodeType.NUMBER);
		assertThat(provider.getNodeType(provider.fromString("\"hello\""))).isEqualTo(JsonNodeType.STRING);
	}

	// ===================
	// Deep Copy Tests
	// ===================

	@Test
	void testDeepCopy() {
		@Var T original = provider.createObject();
		original = provider.set(original, "nested", provider.createObject());
		T nested = requireGet(original, "nested");
		provider.set(nested, "value", provider.createNumber(42));

		T copy = provider.deepCopy(original);

		// Modify the copy's nested object
		T copiedNested = requireGet(copy, "nested");
		provider.set(copiedNested, "value", provider.createNumber(999));

		// Original should be unchanged
		T originalNested = requireGet(original, "nested");
		assertThat(provider.asInt(requireGet(originalNested, "value"))).isEqualTo(42);
	}

	// ===================
	// valueToTree Tests
	// ===================

	@Test
	void testValueToTreePrimitives() {
		assertThat(provider.getNodeType(provider.valueToTree(null))).isEqualTo(JsonNodeType.NULL);
		assertThat(provider.asBoolean(provider.valueToTree(true))).isTrue();
		assertThat(provider.asInt(provider.valueToTree(42))).isEqualTo(42);
		assertThat(provider.asText(provider.valueToTree("hello"))).isEqualTo("hello");
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
		assertThat(provider.asText(node)).isEqualTo("hello\nworld\ttab\"quote");
	}

	@Test
	void testUnicodeStrings() {
		T node = provider.createString("日本語 emoji: \uD83D\uDE00");
		assertThat(provider.asText(node)).isEqualTo("日本語 emoji: \uD83D\uDE00");
	}

	@Test
	void testNestedStructures() throws Exception {
		// Create nested object: {"outer": {"inner": [1, 2, 3]}}
		@Var T inner = provider.createArray();
		inner = provider.add(inner, provider.createNumber(1));
		inner = provider.add(inner, provider.createNumber(2));
		inner = provider.add(inner, provider.createNumber(3));

		@Var T nested = provider.createObject();
		nested = provider.set(nested, "inner", inner);

		@Var T outer = provider.createObject();
		outer = provider.set(outer, "outer", nested);

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
	void testAsTextOnNullNode() {
		// asText on null node should return "null", not empty string
		T node = provider.createNull();
		assertThat(provider.asText(node)).isEqualTo("null");
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
	void testToStringOnNaN() {
		// toString on NaN should return "null" (jq behavior)
		T node = provider.createNumber(Double.NaN);
		String json = provider.toString(node);
		assertThat(json).isEqualTo("null");
	}

	@Test
	void testToStringOnPositiveInfinity() {
		// toString on positive infinity should return the max double value
		T node = provider.createNumber(Double.POSITIVE_INFINITY);
		String json = provider.toString(node);
		assertThat(json).contains("1.7976931348623157e+308");
	}

	@Test
	void testToStringOnNegativeInfinity() {
		// toString on negative infinity should return the negative max double value
		T node = provider.createNumber(Double.NEGATIVE_INFINITY);
		String json = provider.toString(node);
		assertThat(json).contains("-1.7976931348623157e+308");
	}

	@Test
	void testToStringOnWholeNumberDouble() {
		// toString on a whole number double like 0.0 should serialize without decimal (jq behavior)
		T node = provider.createNumber(0.0);
		String json = provider.toString(node);
		assertThat(json).isEqualTo("0");
	}

	@Test
	void testToStringOnNegativeZero() {
		// toString on -0.0 should serialize as "0" (jq behavior)
		T node = provider.createNumber(-0.0);
		String json = provider.toString(node);
		assertThat(json).isEqualTo("0");
	}

	// ================================
	// fromStringStrict Tests
	// ================================

	@Test
	void testFromStringStrictWithEmptyString() {
		// fromStringStrict on empty string should throw exception
		assertThatThrownBy(() -> provider.fromStringStrict(""))
			.isInstanceOf(Exception.class);
	}

	@Test
	void testFromStringStrictWithTrailingContent() {
		// fromStringStrict with trailing content should throw exception
		assertThatThrownBy(() -> provider.fromStringStrict("123 456"))
			.isInstanceOf(Exception.class);
	}

	@Test
	void testFromStringStrictWithWhitespaceOnly() {
		// fromStringStrict on whitespace-only string should throw exception
		assertThatThrownBy(() -> provider.fromStringStrict("   "))
			.isInstanceOf(Exception.class);
	}

	@Test
	void testFromStringStrictWithValidJson() throws Exception {
		// fromStringStrict with valid JSON should work
		T node = provider.fromStringStrict("{\"key\": \"value\"}");
		assertThat(provider.getNodeType(node)).isEqualTo(JsonNodeType.OBJECT);
		assertThat(provider.asText(requireGet(node, "key"))).isEqualTo("value");
	}

	// ================================
	// Object elements() Tests
	// ================================

	@Test
	void testObjectElements() {
		// elements() on an object should return an iterator over the field values
		// This is important for jq functions like from_entries that iterate over object values
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "a", provider.createNumber(1));
		obj = provider.set(obj, "b", provider.createNumber(2));
		obj = provider.set(obj, "c", provider.createNumber(3));

		List<Integer> values = new ArrayList<>();
		Iterator<T> it = provider.elements(obj);
		while (it.hasNext()) {
			values.add(provider.asInt(it.next()));
		}

		assertThat(values).containsExactlyInAnyOrder(1, 2, 3);
	}

	@Test
	void testObjectIterate() {
		// iterate() on an object should also work, returning field values
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "x", provider.createString("foo"));
		obj = provider.set(obj, "y", provider.createString("bar"));

		List<String> values = new ArrayList<>();
		for (T element : provider.iterate(obj)) {
			values.add(provider.asText(element));
		}

		assertThat(values).containsExactlyInAnyOrder("foo", "bar");
	}

	// ================================
	// HTML Character Escaping Tests
	// ================================

	@Test
	void testToStringDoesNotEscapeHtmlCharacters() {
		// toString() should not escape HTML-like characters (<, >, &, ')
		// This is important for jq @json format compatibility
		T node = provider.createString("<>&'\"");
		String json = provider.toString(node);
		// The string should be JSON-escaped for quotes and backslashes,
		// but HTML characters should NOT be Unicode-escaped
		assertThat(json).isEqualTo("\"<>&'\\\"\"");
	}

	@Test
	void testToStringObjectWithHtmlCharacters() {
		// Verify HTML characters in object values are not escaped
		@Var T obj = provider.createObject();
		obj = provider.set(obj, "html", provider.createString("<tag>"));
		String json = provider.toString(obj);
		assertThat(json).contains("\"<tag>\"");
		assertThat(json).doesNotContain("\\u003c"); // Should not Unicode-escape <
	}
}
