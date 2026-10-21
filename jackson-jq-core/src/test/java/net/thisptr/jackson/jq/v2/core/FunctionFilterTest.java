package net.thisptr.jackson.jq.v2.core;

import java.util.Set;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionFilterTest {
	@Test
	public void testAcceptAllAcceptsEverything() {
		FunctionFilter filter = FunctionFilter.acceptAll();

		assertThat(filter.accept(FunctionSignature.of("length", 0))).isTrue();
		assertThat(filter.accept(FunctionSignature.ofVariadic("custom"))).isTrue();
		assertThat(FunctionFilter.acceptAll()).isSameAs(filter);
	}

	@Test
	public void testDenySignaturesRejectsOnlyTheExactSignature() {
		FunctionFilter filter = FunctionFilter.denySignatures(Set.of(FunctionSignature.of("greet", 1)));

		assertThat(filter.accept(FunctionSignature.of("greet", 1))).isFalse();
		assertThat(filter.accept(FunctionSignature.of("greet", 0))).isTrue();
		assertThat(filter.accept(FunctionSignature.of("other", 1))).isTrue();
		assertThat(filter.accept(FunctionSignature.ofVariadic("greet"))).isTrue();
	}

	@Test
	public void testDenySignaturesMatchesAVariadicSignatureOnlyWhenVariadic() {
		FunctionFilter filter = FunctionFilter.denySignatures(Set.of(FunctionSignature.ofVariadic("greet")));

		assertThat(filter.accept(FunctionSignature.ofVariadic("greet"))).isFalse();
		assertThat(filter.accept(FunctionSignature.of("greet", 0))).isTrue();
	}

	@Test
	public void testDenyNamesRejectsEveryArityIncludingVariadic() {
		FunctionFilter filter = FunctionFilter.denyNames(Set.of("greet"));

		assertThat(filter.accept(FunctionSignature.of("greet", 0))).isFalse();
		assertThat(filter.accept(FunctionSignature.of("greet", 3))).isFalse();
		assertThat(filter.accept(FunctionSignature.ofVariadic("greet"))).isFalse();
		assertThat(filter.accept(FunctionSignature.of("other", 0))).isTrue();
	}

	@Test
	public void testDenyFactoriesTakeASnapshotOfTheirArgument() {
		Set<String> names = new java.util.HashSet<>(Set.of("greet"));
		FunctionFilter filter = FunctionFilter.denyNames(names);
		names.clear();

		assertThat(filter.accept(FunctionSignature.of("greet", 0))).isFalse();
	}

	@Test
	public void testAnAllowListCanBeWrittenAsALambda() {
		Set<String> allowed = Set.of("length", "not");
		FunctionFilter filter = signature -> allowed.contains(signature.name());

		assertThat(filter.accept(FunctionSignature.of("length", 0))).isTrue();
		assertThat(filter.accept(FunctionSignature.of("keys", 0))).isFalse();
	}
}
