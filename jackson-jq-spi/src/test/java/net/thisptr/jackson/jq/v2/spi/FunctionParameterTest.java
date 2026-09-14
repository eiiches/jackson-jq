package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FunctionParameterTest {

	@Test
	void testParameterValueOfParsesDollarPrefix() {
		assertEquals(FunctionParameter.ofFilter("f"), FunctionParameter.valueOf("f"));
		assertEquals(FunctionParameter.ofValue("n"), FunctionParameter.valueOf("$n"));
		assertEquals(FunctionParameter.Kind.FILTER, FunctionParameter.valueOf("f").kind());
		assertEquals(FunctionParameter.Kind.VALUE, FunctionParameter.valueOf("$n").kind());
	}

	@ParameterizedTest
	@ValueSource(strings = { "x", "foo", "_foo", "foo_123", "FOO_BAR", "_1", "a" })
	void testValidParameterNames(String name) {
		assertEquals(name, FunctionParameter.ofFilter(name).name());
		assertEquals(name, FunctionParameter.ofValue(name).name());
		assertEquals(name, FunctionParameter.valueOf(name).name());
		assertEquals(name, FunctionParameter.valueOf("$" + name).name());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "123foo", "1", "foo-bar", "foo bar", "$foo", "@foo", "foo.bar" })
	void testInvalidParameterNameThrows(String name) {
		assertThrows(IllegalArgumentException.class, () -> FunctionParameter.ofFilter(name));
		assertThrows(IllegalArgumentException.class, () -> FunctionParameter.ofValue(name));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "$", "$ ", "$$", "$$foo", "$123", "$foo-bar", "$@foo", "123", "foo-bar", "@foo" })
	void testInvalidRawParameterThrows(String raw) {
		assertThrows(IllegalArgumentException.class, () -> FunctionParameter.valueOf(raw));
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullParameterNameThrows() {
		assertThrows(NullPointerException.class, () -> FunctionParameter.ofFilter(null));
		assertThrows(NullPointerException.class, () -> FunctionParameter.ofValue(null));
		assertThrows(NullPointerException.class, () -> FunctionParameter.valueOf(null));
	}

	@Test
	void testParameterEqualsAndHashCode() {
		assertEquals(FunctionParameter.ofFilter("f"), FunctionParameter.ofFilter("f"));
		assertEquals(FunctionParameter.ofFilter("f").hashCode(), FunctionParameter.ofFilter("f").hashCode());
		assertNotEquals(FunctionParameter.ofFilter("f"), FunctionParameter.ofValue("f"));
		assertNotEquals(FunctionParameter.ofFilter("f"), FunctionParameter.ofFilter("g"));
		assertNotEquals(FunctionParameter.ofFilter("f"), null);
		assertNotEquals(FunctionParameter.ofFilter("f"), "f");
	}

	@Test
	void testParameterToString() {
		assertEquals("f", FunctionParameter.ofFilter("f").toString());
		assertEquals("$n", FunctionParameter.ofValue("n").toString());
	}
}
