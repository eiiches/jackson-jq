package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		assertThat(sig.name()).isEqualTo(name);
		assertThat(sig.arity()).isEqualTo(0);
		assertThat(sig.isVariadic()).isFalse();
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
		assertThatThrownBy(() -> FunctionSignature.of(name, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@SuppressWarnings("NullAway")
	void testNullFunctionName() {
		assertThatThrownBy(() -> FunctionSignature.of(null, 0)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testValidArity() {
		FunctionSignature sig0 = FunctionSignature.of("foo", 0);
		assertThat(sig0.arity()).isEqualTo(0);
		assertThat(sig0.isVariadic()).isFalse();

		FunctionSignature sig5 = FunctionSignature.of("foo", 5);
		assertThat(sig5.arity()).isEqualTo(5);
		assertThat(sig5.isVariadic()).isFalse();
	}

	@Test
	void testNegativeArityThrows() {
		assertThatThrownBy(() -> FunctionSignature.of("foo", -1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.of("foo", -10)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testWithArity() {
		FunctionSignature sig = FunctionSignature.of("foo", 2);
		assertThat(sig.arity()).isEqualTo(2);
		assertThat(sig.isVariadic()).isFalse();

		FunctionSignature sig3 = sig.withArity(3);
		assertThat(sig3.arity()).isEqualTo(3);
		assertThat(sig3.isVariadic()).isFalse();

		assertThatThrownBy(() -> sig.withArity(-1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testAsVariadic() {
		FunctionSignature sig = FunctionSignature.of("foo", 2);
		FunctionSignature variadic = sig.asVariadic();
		assertThat(variadic.name()).isEqualTo("foo");
		assertThat(variadic.arity()).isNull();
		assertThat(variadic.isVariadic()).isTrue();
		assertThat(variadic.asVariadic()).isSameAs(variadic);
	}

	@Test
	void testToString() {
		assertThat(FunctionSignature.of("length", 0)).hasToString("length/0");
		assertThat(FunctionSignature.of("map", 1)).hasToString("map/1");
		assertThat(FunctionSignature.of("@csv", 0)).hasToString("@csv/0");
		assertThat(FunctionSignature.of("custom", 0).asVariadic()).hasToString("custom/*");
		assertThat(FunctionSignature.ofVariadic("custom")).hasToString("custom/*");
	}

	@Test
	void testEqualsAndHashCode() {
		FunctionSignature sig1 = FunctionSignature.of("foo", 1);
		FunctionSignature sig2 = FunctionSignature.of("foo", 1);
		FunctionSignature sig3 = FunctionSignature.of("foo", 2);
		FunctionSignature sig4 = FunctionSignature.of("bar", 1);
		FunctionSignature variadic1 = FunctionSignature.ofVariadic("foo");
		FunctionSignature variadic2 = FunctionSignature.of("foo", 1).asVariadic();

		assertThat(sig1)
				.isEqualTo(sig2)
				.hasSameHashCodeAs(sig2)
				.isNotEqualTo(sig3)
				.isNotEqualTo(sig4)
				.isNotEqualTo(variadic1)
				.isNotNull()
				.isNotEqualTo("foo/1");
		assertThat(variadic1)
				.isEqualTo(variadic2)
				.hasSameHashCodeAs(variadic2);
	}

	@Test
	void testOfVariadic() {
		FunctionSignature sig = FunctionSignature.ofVariadic("custom");
		assertThat(sig.name()).isEqualTo("custom");
		assertThat(sig.arity()).isNull();
		assertThat(sig.isVariadic()).isTrue();
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullValueOf() {
		assertThatThrownBy(() -> FunctionSignature.valueOf(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testValueOf() {
		assertThat(FunctionSignature.valueOf("length/0")).isEqualTo(FunctionSignature.of("length", 0));
		assertThat(FunctionSignature.valueOf("map/1")).isEqualTo(FunctionSignature.of("map", 1));
		assertThat(FunctionSignature.valueOf("@csv/0")).isEqualTo(FunctionSignature.of("@csv", 0));
		assertThat(FunctionSignature.valueOf("custom/*")).isEqualTo(FunctionSignature.ofVariadic("custom"));

		assertThatThrownBy(() -> FunctionSignature.valueOf("")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("custom")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/bar")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/-1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/+1")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/01")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("foo/00")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionSignature.valueOf("123foo/0")).isInstanceOf(IllegalArgumentException.class);
	}
}
