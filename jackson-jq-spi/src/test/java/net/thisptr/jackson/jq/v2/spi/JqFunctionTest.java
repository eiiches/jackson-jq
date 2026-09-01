package net.thisptr.jackson.jq.v2.spi;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JqFunctionTest {

	@Test
	void testAccessors() {
		VersionRange version = VersionRange.valueOf("[1.6, )");
		JqFunction fn = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("f"), FunctionParameter.ofValue("n")), "f + $n", version);

		assertEquals("f", fn.name());
		assertEquals(FunctionSignature.of("f", 2), fn.signature());
		assertEquals(Arrays.asList(FunctionParameter.ofFilter("f"), FunctionParameter.ofValue("n")), fn.parameters());
		assertEquals("f + $n", fn.body());
		assertEquals(version, fn.version());
	}

	@Test
	void testSignatureArityMatchesArgCount() {
		assertEquals(FunctionSignature.of("f", 0), JqFunction.of("f", Collections.emptyList(), ".").signature());
		assertEquals(FunctionSignature.of("f", 3), JqFunction.of("f", Arrays.asList(FunctionParameter.valueOf("a"), FunctionParameter.valueOf("$b"), FunctionParameter.valueOf("c")), ".").signature());
	}

	@Test
	void testNullVersionMeansAllVersions() {
		JqFunction fn = JqFunction.of("f", Collections.emptyList(), ".");
		assertNull(fn.version());
	}

	@Test
	void testParametersIsUnmodifiable() {
		JqFunction fn = JqFunction.of("f", Collections.singletonList(FunctionParameter.ofFilter("x")), ".");
		assertThrows(UnsupportedOperationException.class, () -> fn.parameters().add(FunctionParameter.ofFilter("y")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "123foo", "foo-bar", "foo bar", "$foo" })
	void testInvalidNameThrows(String name) {
		assertThrows(IllegalArgumentException.class, () -> JqFunction.of(name, Collections.emptyList(), "."));
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullNameThrows() {
		assertThrows(NullPointerException.class, () -> JqFunction.of(null, Collections.emptyList(), "."));
	}

	@Test
	void testFilterAndValueArgsAreAccepted() {
		JqFunction fn = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("filterArg"), FunctionParameter.ofValue("valueArg")), ".");
		assertEquals(FunctionParameter.Kind.FILTER, fn.parameters().get(0).kind());
		assertEquals(FunctionParameter.Kind.VALUE, fn.parameters().get(1).kind());
	}

	@Test
	void testEqualsAndHashCode() {
		VersionRange v1 = VersionRange.valueOf("[1.6, )");
		VersionRange v2 = VersionRange.valueOf("[1.7, )");
		JqFunction fn1 = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);
		JqFunction fn2 = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);
		JqFunction fnDiffVersion = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v2);
		JqFunction fnDiffBody = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a * $b", v1);
		JqFunction fnDiffParams = JqFunction.of("f", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofFilter("b")), "a + $b", v1);
		JqFunction fnDiffName = JqFunction.of("g", Arrays.asList(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);

		assertEquals(fn1, fn2);
		assertEquals(fn1.hashCode(), fn2.hashCode());
		assertNotEquals(fn1, fnDiffVersion);
		assertNotEquals(fn1, fnDiffBody);
		assertNotEquals(fn1, fnDiffParams);
		assertNotEquals(fn1, fnDiffName);
		assertNotEquals(fn1, null);
		assertNotEquals(fn1, "f");
	}

	@Test
	void testToString() {
		assertEquals("def length: _length;", JqFunction.of("length", Collections.emptyList(), "_length").toString());
		assertEquals("def map(f): [.[] | f];", JqFunction.of("map", Collections.singletonList(FunctionParameter.ofFilter("f")), "[.[] | f]").toString());
		assertEquals("def limit($n; exp): ...; # [1.6.0, )",
				JqFunction.of("limit", Arrays.asList(FunctionParameter.ofValue("n"), FunctionParameter.ofFilter("exp")), "...", VersionRange.valueOf("[1.6, )")).toString());
	}
}
