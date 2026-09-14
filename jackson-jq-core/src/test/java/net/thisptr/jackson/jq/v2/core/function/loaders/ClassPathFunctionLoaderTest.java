package net.thisptr.jackson.jq.v2.core.function.loaders;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

import static org.assertj.core.api.Assertions.assertThat;

public class ClassPathFunctionLoaderTest {

	@FunctionRegistration(name = "fixed", nargs = 2)
	@FunctionRegistration(name = "variadic", nargs = -1)
	private static class Fixture {
	}

	private static FunctionRegistration registrationNamed(String name) {
		for (FunctionRegistration reg : Fixture.class.getAnnotationsByType(FunctionRegistration.class))
			if (reg.name().equals(name))
				return reg;
		throw new AssertionError("no such registration: " + name);
	}

	@Test
	public void nonNegativeNargsProducesFixedAritySignature() {
		assertThat(ClassPathFunctionLoader.signatureOf(registrationNamed("fixed")))
				.isEqualTo(FunctionSignature.of("fixed", 2));
	}

	@Test
	public void negativeNargsProducesVariadicSignature() {
		assertThat(ClassPathFunctionLoader.signatureOf(registrationNamed("variadic")))
				.isEqualTo(FunctionSignature.ofVariadic("variadic"));
	}
}
