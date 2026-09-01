package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertFalse(sig.isVariadic());
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
		assertFalse(sig0.isVariadic());

		FunctionSignature sig5 = FunctionSignature.of("foo", 5);
		assertEquals(5, sig5.arity());
		assertFalse(sig5.isVariadic());
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
		assertFalse(sig.isVariadic());

		FunctionSignature sig3 = sig.withArity(3);
		assertEquals(3, sig3.arity());
		assertFalse(sig3.isVariadic());

		assertThrows(IllegalArgumentException.class, () -> sig.withArity(-1));
	}

	@Test
	void testAsVariadic() {
		FunctionSignature sig = FunctionSignature.of("foo", 2);
		FunctionSignature variadic = sig.asVariadic();
		assertEquals("foo", variadic.name());
		assertNull(variadic.arity());
		assertTrue(variadic.isVariadic());
		assertSame(variadic, variadic.asVariadic());
	}

	@Test
	void testToString() {
		assertEquals("length/0", FunctionSignature.of("length", 0).toString());
		assertEquals("map/1", FunctionSignature.of("map", 1).toString());
		assertEquals("@csv/0", FunctionSignature.of("@csv", 0).toString());
		assertEquals("custom/*", FunctionSignature.of("custom", 0).asVariadic().toString());
		assertEquals("custom/*", FunctionSignature.ofVariadic("custom").toString());
	}

	@Test
	void testEqualsAndHashCode() {
		FunctionSignature sig1 = FunctionSignature.of("foo", 1);
		FunctionSignature sig2 = FunctionSignature.of("foo", 1);
		FunctionSignature sig3 = FunctionSignature.of("foo", 2);
		FunctionSignature sig4 = FunctionSignature.of("bar", 1);
		FunctionSignature variadic1 = FunctionSignature.ofVariadic("foo");
		FunctionSignature variadic2 = FunctionSignature.of("foo", 1).asVariadic();

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

	@Test
	void testOfVariadic() {
		FunctionSignature sig = FunctionSignature.ofVariadic("custom");
		assertEquals("custom", sig.name());
		assertNull(sig.arity());
		assertTrue(sig.isVariadic());
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullValueOf() {
		assertThrows(NullPointerException.class, () -> FunctionSignature.valueOf(null));
	}

	@Test
	void testValueOf() {
		assertEquals(FunctionSignature.of("length", 0), FunctionSignature.valueOf("length/0"));
		assertEquals(FunctionSignature.of("map", 1), FunctionSignature.valueOf("map/1"));
		assertEquals(FunctionSignature.of("@csv", 0), FunctionSignature.valueOf("@csv/0"));
		assertEquals(FunctionSignature.ofVariadic("custom"), FunctionSignature.valueOf("custom/*"));

		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("custom"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/bar"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/-1"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/+1"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/01"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("foo/00"));
		assertThrows(IllegalArgumentException.class, () -> FunctionSignature.valueOf("123foo/0"));
	}
}
