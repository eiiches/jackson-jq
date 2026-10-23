package net.thisptr.jackson.jq.v2.core;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionContext;
import net.thisptr.jackson.jq.v2.core.function.FunctionFilter;
import net.thisptr.jackson.jq.v2.core.function.FunctionOrigin;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionFilterTest {
	private static FunctionContext context(String name, int arity) {
		return FunctionContext.of(FunctionSignature.of(name, arity), FunctionOrigin.FUNCTION_LOADER, "");
	}

	private static FunctionContext context(String name, int arity, FunctionOrigin origin, String locator) {
		return FunctionContext.of(FunctionSignature.of(name, arity), origin, locator);
	}

	private static FunctionContext variadicContext(String name) {
		return FunctionContext.of(FunctionSignature.ofVariadic(name), FunctionOrigin.FUNCTION_LOADER, "");
	}

	@Test
	public void testAcceptAllAcceptsEverything() {
		FunctionFilter filter = FunctionFilter.acceptAll();

		assertThat(filter.accept(context("length", 0))).isTrue();
		assertThat(filter.accept(variadicContext("custom"))).isTrue();
		assertThat(FunctionFilter.acceptAll()).isSameAs(filter);
	}

	@Test
	public void testDenySignaturesRejectsOnlyTheExactSignature() {
		FunctionFilter filter = FunctionFilter.denySignatures(Set.of(FunctionSignature.of("greet", 1)));

		assertThat(filter.accept(context("greet", 1))).isFalse();
		assertThat(filter.accept(context("greet", 0))).isTrue();
		assertThat(filter.accept(context("other", 1))).isTrue();
		assertThat(filter.accept(variadicContext("greet"))).isTrue();
	}

	@Test
	public void testDenySignaturesMatchesAVariadicSignatureOnlyWhenVariadic() {
		FunctionFilter filter = FunctionFilter.denySignatures(Set.of(FunctionSignature.ofVariadic("greet")));

		assertThat(filter.accept(variadicContext("greet"))).isFalse();
		assertThat(filter.accept(context("greet", 0))).isTrue();
	}

	@Test
	public void testDenyNamesRejectsEveryArityIncludingVariadic() {
		FunctionFilter filter = FunctionFilter.denyNames(Set.of("greet"));

		assertThat(filter.accept(context("greet", 0))).isFalse();
		assertThat(filter.accept(context("greet", 3))).isFalse();
		assertThat(filter.accept(variadicContext("greet"))).isFalse();
		assertThat(filter.accept(context("other", 0))).isTrue();
	}

	@Test
	public void testDenyFactoriesTakeASnapshotOfTheirArgument() {
		Set<String> names = new HashSet<>(Set.of("greet"));
		FunctionFilter filter = FunctionFilter.denyNames(names);
		names.clear();

		assertThat(filter.accept(context("greet", 0))).isFalse();
	}

	@Test
	public void testAnAllowListCanBeWrittenAsALambda() {
		Set<String> allowed = Set.of("length", "not");
		FunctionFilter filter = ctx -> allowed.contains(ctx.getSignature().name());

		assertThat(filter.accept(context("length", 0))).isTrue();
		assertThat(filter.accept(context("keys", 0))).isFalse();
	}

	@Test
	public void testFilteringByOriginAndLocator() {
		FunctionFilter filter = ctx -> !(ctx.getOrigin() == FunctionOrigin.PATH_IMPORT && ctx.getLocator().equals("blocked/module"));

		assertThat(filter.accept(context("helper", 0, FunctionOrigin.PATH_IMPORT, "blocked/module"))).isFalse();
		assertThat(filter.accept(context("helper", 0, FunctionOrigin.PATH_IMPORT, "allowed/module"))).isTrue();
		assertThat(filter.accept(context("helper", 0, FunctionOrigin.FUNCTION_LOADER, ""))).isTrue();
	}
}
