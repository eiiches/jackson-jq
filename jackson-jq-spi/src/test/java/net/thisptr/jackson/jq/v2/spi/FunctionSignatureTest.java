package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FunctionSignatureTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"foo",
			"_foo",
			"foo_bar",
			"foo123",
			"_123",
			"_",
			"A",
			"Z_9",
			"@csv",
			"@base64",
			"@base64d",
			"@html",
			"@json",
			"@sh",
			"@text",
			"@tsv",
			"@uri",
			"@_custom",
			"@custom123"
	})
	void testValidFunctionNames(String name) {
		FunctionSignature sig = FunctionSignature.of(name, 0);
		assertEquals(name, sig.name());
		assertEquals(0, sig.arity());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"123foo",
			"9",
			"@123",
			"@1foo",
			"@",
			"foo-bar",
			"foo.bar",
			"foo::bar",
			"foo/bar",
			"foo bar",
			"$foo",
			"@foo-bar"
	})
	void testInvalidFunctionNames(String name) {
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.of(name, 0));
	}

	@Test
	@SuppressWarnings("NullAway")
	void testNullFunctionName() {
		assertThrows(NullPointerException.class, () -> FunctionSignature.of(null, 0));
	}

	@Test
	void testValidArity() {
		FunctionSignature sig0 = FunctionSignature.of("foo", 0);
		assertEquals(0, sig0.arity());

		FunctionSignature sig5 = FunctionSignature.of("foo", 5);
		assertEquals(5, sig5.arity());
	}

	@Test
	void testNegativeArityThrows() {
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.of("foo", -1));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.of("foo", -10));
	}

	@Test
	void testWithArity() {
		FunctionSignature sig = FunctionSignature.of("foo", 2);
		assertEquals(2, sig.arity());

		FunctionSignature variadic = sig.withArity(null);
		assertEquals("foo", variadic.name());
		assertNull(variadic.arity());

		FunctionSignature sig3 = sig.withArity(3);
		assertEquals(3, sig3.arity());

		assertThrows(IllegalArgumentException.class, () -> sig.withArity(-1));
	}

	@Test
	void testToString() {
		assertEquals("length/0", FunctionSignature.of("length", 0).toString());
		assertEquals("map/1", FunctionSignature.of("map", 1).toString());
		assertEquals("@csv/0", FunctionSignature.of("@csv", 0).toString());
		assertEquals("custom", FunctionSignature.of("custom", 0).withArity(null).toString());
	}

	@Test
	void testEqualsAndHashCode() {
		FunctionSignature sig1 = FunctionSignature.of("foo", 1);
		FunctionSignature sig2 = FunctionSignature.of("foo", 1);
		FunctionSignature sig3 = FunctionSignature.of("foo", 2);
		FunctionSignature sig4 = FunctionSignature.of("bar", 1);
		FunctionSignature variadic1 = FunctionSignature.of("foo", 0).withArity(null);
		FunctionSignature variadic2 = FunctionSignature.of("foo", 1).withArity(null);

		assertEquals(sig1, sig2);
		assertEquals(sig1.hashCode(), sig2.hashCode());
		assertEquals(variadic1, variadic2);
		assertEquals(variadic1.hashCode(), variadic2.hashCode());

		assertNotEquals(sig1, sig3);
		assertNotEquals(sig1, sig4);
		assertNotEquals(sig1, variadic1);
		assertNotEquals(sig1, null);
		assertNotEquals(sig1, "foo/1");
	}
}
