package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every registered Java builtin must publish signatures that fit the arity it is registered under.
 * An arity typo would otherwise leave a builtin with no applicable overload, silently turning every
 * call to it into a type error.
 */
class BuiltinTypeSchemeCoverageTest {
	@Test
	void everyRegisteredBuiltinDeclaresSchemesForItsArity() {
		List<String> problems = new ArrayList<>();
		for (Version jqVersion : Versions.versions()) {
			Map<FunctionSignature, Function> functions = ClassPathFunctionLoader.getInstance().getFunctions(jqVersion);
			// Guards against the check passing vacuously if discovery ever stops finding the builtins.
			assertThat(functions).describedAs("registered builtins for jq %s", jqVersion)
					.hasSizeGreaterThan(70)
					.containsKeys(FunctionSignature.of("length", 0), FunctionSignature.of("sort_by", 1),
							FunctionSignature.of("range", 3), FunctionSignature.of("floor", 0));
			for (Map.Entry<FunctionSignature, Function> entry : functions.entrySet()) {
				FunctionSignature signature = entry.getKey();
				Integer arity = signature.arity();
				if (arity == null) // variadic: no single arity to check
					continue;
				List<TypeScheme<FunctionType>> schemes = entry.getValue().types(jqVersion, arity);
				String where = signature + " (" + entry.getValue().getClass().getName() + ") on jq " + jqVersion;
				if (schemes.isEmpty()) {
					problems.add(where + ": no type schemes");
					continue;
				}
				for (TypeScheme<FunctionType> scheme : schemes) {
					int declared = scheme.body().parameterTypes().size();
					if (declared != arity)
						problems.add(where + ": scheme declares " + declared + " parameters");
				}
			}
		}
		assertThat(problems).isEmpty();
	}
}
