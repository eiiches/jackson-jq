package net.thisptr.jackson.jq.v2.core.function.loaders;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.FunctionLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.JqFunction;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The builtins are registered in code, so this loader answers the same way whatever the classpath
 * happens to contain and however the jar was packaged.
 */
public class BuiltinFunctionLoaderTest {
	private final FunctionLoader loader = BuiltinFunctionLoader.getInstance();

	@Test
	public void suppliesJavaBuiltins() {
		Map<FunctionSignature, Function> functions = loader.getFunctions(Versions.JQ_1_6);

		assertThat(functions)
				.containsKeys(FunctionSignature.of("length", 0), FunctionSignature.of("range", 3),
						FunctionSignature.of("floor", 0), FunctionSignature.of("@csv", 0),
						FunctionSignature.of("builtins", 0))
				.doesNotContainKey(FunctionSignature.of("map", 1));
	}

	@Test
	public void suppliesJqBuiltins() {
		Map<FunctionSignature, JqFunction> jqFunctions = loader.getJqFunctions(Versions.JQ_1_6);

		assertThat(jqFunctions)
				.containsKeys(FunctionSignature.of("map", 1), FunctionSignature.of("select", 1))
				.doesNotContainKey(FunctionSignature.of("length", 0));
	}

	/**
	 * A registration states the jq versions it applies to, and the registry is built per version --
	 * {@code isempty/1} arrived in 1.6, and {@code ltrimstr/1} stopped being a Java builtin in 1.8.0,
	 * where {@code CoreJqLibrary} took it over.
	 */
	@Test
	public void appliesVersionRanges() {
		assertThat(loader.getFunctions(Versions.JQ_1_6)).containsKey(FunctionSignature.of("isempty", 1));
		assertThat(loader.getFunctions(Versions.JQ_1_5)).doesNotContainKey(FunctionSignature.of("isempty", 1));

		assertThat(loader.getFunctions(Versions.JQ_1_7)).containsKey(FunctionSignature.of("ltrimstr", 1));
		assertThat(loader.getJqFunctions(Versions.JQ_1_7)).doesNotContainKey(FunctionSignature.of("ltrimstr", 1));
		assertThat(loader.getFunctions(Versions.JQ_1_8_0)).doesNotContainKey(FunctionSignature.of("ltrimstr", 1));
		assertThat(loader.getJqFunctions(Versions.JQ_1_8_0)).containsKey(FunctionSignature.of("ltrimstr", 1));
	}

	/**
	 * The registry is closed, so a signature claimed twice is a bug rather than an ambiguity to resolve;
	 * {@link BuiltinFunctionLoader} rejects one outright. Building every supported version's registry is
	 * what proves none of them does.
	 */
	@Test
	public void registersEachSignatureOncePerVersion() {
		for (Version jqVersion : Versions.versions()) {
			assertThat(loader.getFunctions(jqVersion)).isNotEmpty();
			assertThat(loader.getJqFunctions(jqVersion)).isNotEmpty();
		}
	}

	/**
	 * Every {@code Function} instance is shared across versions and across callers, so a builtin must not
	 * keep per-query state on itself. Asking twice must hand back the very same objects.
	 */
	@Test
	public void functionInstancesAreShared() {
		Map<FunctionSignature, Function> first = loader.getFunctions(Versions.JQ_1_8_2);
		Map<FunctionSignature, Function> second = loader.getFunctions(Versions.JQ_1_8_2);

		assertThat(second).containsOnlyKeys(first.keySet());
		for (Map.Entry<FunctionSignature, Function> entry : first.entrySet())
			assertThat(second.get(entry.getKey())).isSameAs(entry.getValue());
	}

	@FunctionRegistration(name = "fixed", nargs = 2)
	@FunctionRegistration(name = "variadic", nargs = -1)
	private static class ArityFixture {
	}

	private static FunctionRegistration registrationNamed(String name) {
		for (FunctionRegistration reg : ArityFixture.class.getAnnotationsByType(FunctionRegistration.class))
			if (reg.name().equals(name))
				return reg;
		throw new AssertionError("no such registration: " + name);
	}

	@Test
	public void nonNegativeNargsProducesFixedAritySignature() {
		assertThat(BuiltinFunctionLoader.signatureOf(registrationNamed("fixed")))
				.isEqualTo(FunctionSignature.of("fixed", 2));
	}

	@Test
	public void negativeNargsProducesVariadicSignature() {
		assertThat(BuiltinFunctionLoader.signatureOf(registrationNamed("variadic")))
				.isEqualTo(FunctionSignature.ofVariadic("variadic"));
	}

	/**
	 * {@code builtins/0} reports this loader's own names, and reports itself among them.
	 */
	@Test
	public void builtinsFunctionIsRegisteredAlongsideTheNamesItReports() {
		Set<FunctionSignature> signatures = new HashSet<>(loader.getFunctions(Versions.JQ_1_8_2).keySet());
		signatures.addAll(loader.getJqFunctions(Versions.JQ_1_8_2).keySet());

		assertThat(signatures).contains(FunctionSignature.of("builtins", 0));
		assertThat(signatures).hasSizeGreaterThan(70);
	}
}
