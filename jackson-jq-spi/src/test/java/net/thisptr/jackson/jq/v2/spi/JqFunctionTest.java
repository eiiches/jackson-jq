package net.thisptr.jackson.jq.v2.spi;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JqFunctionTest {

	@Test
	void testAccessors() {
		VersionRange version = VersionRange.valueOf("[1.6, )");
		JqFunction fn = JqFunction.of("f", List.of(FunctionParameter.ofFilter("f"), FunctionParameter.ofValue("n")), "f + $n", version);

		assertThat(fn.name()).isEqualTo("f");
		assertThat(fn.signature()).isEqualTo(FunctionSignature.of("f", 2));
		assertThat(fn.parameters()).isEqualTo(List.of(FunctionParameter.ofFilter("f"), FunctionParameter.ofValue("n")));
		assertThat(fn.body()).isEqualTo("f + $n");
		assertThat(fn.version()).isEqualTo(version);
	}

	@Test
	void testSignatureArityMatchesArgCount() {
		assertThat(JqFunction.of("f", Collections.emptyList(), ".").signature()).isEqualTo(FunctionSignature.of("f", 0));
		assertThat(JqFunction.of("f", List.of(FunctionParameter.valueOf("a"), FunctionParameter.valueOf("$b"), FunctionParameter.valueOf("c")), ".").signature()).isEqualTo(FunctionSignature.of("f", 3));
	}

	@Test
	void testNullVersionMeansAllVersions() {
		JqFunction fn = JqFunction.of("f", Collections.emptyList(), ".");
		assertThat(fn.version()).isNull();
	}

	@Test
	void testParametersIsUnmodifiable() {
		JqFunction fn = JqFunction.of("f", Collections.singletonList(FunctionParameter.ofFilter("x")), ".");
		assertThatThrownBy(() -> fn.parameters().add(FunctionParameter.ofFilter("y"))).isInstanceOf(UnsupportedOperationException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\t", "123foo", "foo-bar", "foo bar", "$foo" })
	void testInvalidNameThrows(String name) {
		assertThatThrownBy(() -> JqFunction.of(name, Collections.emptyList(), ".")).isInstanceOf(IllegalArgumentException.class);
	}

	// NullAway checks for null arguments; this test verifies runtime null rejection.
	@Test
	@SuppressWarnings("NullAway")
	void testNullNameThrows() {
		assertThatThrownBy(() -> JqFunction.of(null, Collections.emptyList(), ".")).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testFilterAndValueArgsAreAccepted() {
		JqFunction fn = JqFunction.of("f", List.of(FunctionParameter.ofFilter("filterArg"), FunctionParameter.ofValue("valueArg")), ".");
		assertThat(fn.parameters().get(0).kind()).isEqualTo(FunctionParameter.Kind.FILTER);
		assertThat(fn.parameters().get(1).kind()).isEqualTo(FunctionParameter.Kind.VALUE);
	}

	@Test
	void testEqualsAndHashCode() {
		VersionRange v1 = VersionRange.valueOf("[1.6, )");
		VersionRange v2 = VersionRange.valueOf("[1.7, )");
		JqFunction fn1 = JqFunction.of("f", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);
		JqFunction fn2 = JqFunction.of("f", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);
		JqFunction fnDiffVersion = JqFunction.of("f", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v2);
		JqFunction fnDiffBody = JqFunction.of("f", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a * $b", v1);
		JqFunction fnDiffParams = JqFunction.of("f", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofFilter("b")), "a + $b", v1);
		JqFunction fnDiffName = JqFunction.of("g", List.of(FunctionParameter.ofFilter("a"), FunctionParameter.ofValue("b")), "a + $b", v1);

		assertThat(fn1)
				.isEqualTo(fn2)
				.hasSameHashCodeAs(fn2)
				.isNotEqualTo(fnDiffVersion)
				.isNotEqualTo(fnDiffBody)
				.isNotEqualTo(fnDiffParams)
				.isNotEqualTo(fnDiffName)
				.isNotNull()
				.isNotEqualTo("f");
	}

	@Test
	void testToString() {
		assertThat(JqFunction.of("length", Collections.emptyList(), "_length")).hasToString("def length: _length;");
		assertThat(JqFunction.of("map", Collections.singletonList(FunctionParameter.ofFilter("f")), "[.[] | f]")).hasToString("def map(f): [.[] | f];");
		assertThat(JqFunction.of("limit", List.of(FunctionParameter.ofValue("n"), FunctionParameter.ofFilter("exp")), "...", VersionRange.valueOf("[1.6, )")))
				.hasToString("def limit($n; exp): ...; # [1.6.0, )");
	}
}
