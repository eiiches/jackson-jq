package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FunctionParameterTest {

	@Test
	void testParameterValueOfParsesDollarPrefix() {
		assertThat(FunctionParameter.valueOf("f")).isEqualTo(FunctionParameter.ofFilter("f"));
		assertThat(FunctionParameter.valueOf("$n")).isEqualTo(FunctionParameter.ofValue("n"));
		assertThat(FunctionParameter.valueOf("f").kind()).isEqualTo(FunctionParameter.Kind.FILTER);
		assertThat(FunctionParameter.valueOf("$n").kind()).isEqualTo(FunctionParameter.Kind.VALUE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "x", "foo", "_foo", "foo_123", "FOO_BAR", "_1", "a" })
	void testValidParameterNames(String name) {
		assertThat(FunctionParameter.ofFilter(name).name()).isEqualTo(name);
		assertThat(FunctionParameter.ofValue(name).name()).isEqualTo(name);
		assertThat(FunctionParameter.valueOf(name).name()).isEqualTo(name);
		assertThat(FunctionParameter.valueOf("$" + name).name()).isEqualTo(name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "123foo", "1", "foo-bar", "foo bar", "$foo", "@foo", "foo.bar" })
	void testInvalidParameterNameThrows(String name) {
		assertThatThrownBy(() -> FunctionParameter.ofFilter(name)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FunctionParameter.ofValue(name)).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "$", "$ ", "$$", "$$foo", "$123", "$foo-bar", "$@foo", "123", "foo-bar", "@foo" })
	void testInvalidRawParameterThrows(String raw) {
		assertThatThrownBy(() -> FunctionParameter.valueOf(raw)).isInstanceOf(IllegalArgumentException.class);
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullParameterNameThrows() {
		assertThatThrownBy(() -> FunctionParameter.ofFilter(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> FunctionParameter.ofValue(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> FunctionParameter.valueOf(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testParameterEqualsAndHashCode() {
		FunctionParameter filterF = FunctionParameter.ofFilter("f");
		assertThat(filterF)
				.isEqualTo(FunctionParameter.ofFilter("f"))
				.hasSameHashCodeAs(FunctionParameter.ofFilter("f"))
				.isNotEqualTo(FunctionParameter.ofValue("f"))
				.isNotEqualTo(FunctionParameter.ofFilter("g"))
				.isNotNull()
				.isNotEqualTo("f");
	}

	@Test
	void testParameterToString() {
		assertThat(FunctionParameter.ofFilter("f")).hasToString("f");
		assertThat(FunctionParameter.ofValue("n")).hasToString("$n");
	}
}
