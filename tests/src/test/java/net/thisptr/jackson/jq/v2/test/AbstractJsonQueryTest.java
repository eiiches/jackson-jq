package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ChainedModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.comparator.TestJsonNodeComparator;
import net.thisptr.jackson.jq.v2.test.testcase.ModuleFixtures;
import net.thisptr.jackson.jq.v2.test.testcase.TestCase;
import net.thisptr.jackson.jq.v2.test.testcase.TestCaseLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Abstract base class for JsonQuery tests. Subclasses must implement methods to provide
 * the JsonProvider-specific environment and comparator.
 *
 * <p>This class is designed to be extended by JSON provider implementations (e.g., jackson-jq-jackson2)
 * to run the standard test suite against their implementation.
 *
 * @param <T> The JSON node type used by the JsonProvider implementation
 */
public abstract class AbstractJsonQueryTest<T> {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * The JsonProvider under test.
	 *
	 * @return The provider implementation to run the test suite against
	 */
	protected abstract JsonProvider<T> getJsonProvider();

	/**
	 * Parse a Jackson JsonNode (from test data) to the provider's native type.
	 *
	 * @param node The Jackson JsonNode from test data
	 * @return The equivalent node in the provider's type
	 */
	protected abstract T parseTestNode(JsonNode node);

	private void test(TestCase tc, Version version, @Nullable Path moduleSearchPath) throws Throwable {
		EnvironmentBuilder<T> envBuilder = new EnvironmentBuilder<>(getJsonProvider(), version);
		if (moduleSearchPath != null) {
			envBuilder.setModuleLoader(new ChainedModuleLoader<>(
					new FileSystemModuleLoader<>(envBuilder.getJsonProvider(), version, moduleSearchPath),
					ClassPathModuleLoader.getInstance()));
		}
		Environment<T> env = envBuilder
				.defineVariable("ENV", () -> envBuilder.getJsonProvider().createObject(Collections.singletonMap("PAGER", envBuilder.getJsonProvider().createString("less"))))
				.build();

		String command = String.format("jq (v%s) '%s' <<< '%s'", version, tc.q, tc.in);

		if (!tc.shouldCompile) {
			assertThrows(JsonQueryException.class, () -> env.compile(tc.q));
			return;
		}

		// Convert test data from Jackson JsonNode to provider's type
		T input = parseTestNode(tc.in);
		List<T> expectedOut = new ArrayList<>();
		for (JsonNode outNode : tc.out) {
			expectedOut.add(parseTestNode(outNode));
		}

		Comparator<T> comparator = new TestJsonNodeComparator<>(getJsonProvider(), true, tc.numericalErrors);

		@Var boolean failed = false;
		try {
			JsonQuery<T> q = env.compile(tc.q);
			List<T> out = new ArrayList<>();
			q.apply(input, out::add);
			assertThat(out).as("%s", command)
					.usingElementComparator(comparator)
					.isEqualTo(expectedOut);
		} catch (Throwable e) {
			failed = true;
			if (!Boolean.TRUE.equals(tc.failing)) {
				if (e instanceof AssertionError)
					throw e;
				e.addSuppressed(new RuntimeException("NOTE: " + command));
				throw e;
			}
		}

		if (Boolean.TRUE.equals(tc.failing))
			assertThat(failed).describedAs("The test case is marked as failing but completed successfully: %s", command).isTrue();
	}

	protected final void run(String[] args) throws IOException {
		if (args.length != 1)
			throw new IllegalArgumentException(String.format("Usage: %s <test-case-resource>", getClass().getSimpleName()));

		Assertions.assertAll(
				args[0],
				TestCaseLoader.loadTestCasesAsJsonStrings(args[0]).parallel()
						.flatMap(tcText -> Versions.versions().stream()
								.map(version -> (Executable) () -> testVersion(tcText, version))));
	}

	private void testVersion(String tcText, Version jqVersion) throws Throwable {
		TestCase tc = JSON_MAPPER.readValue(tcText, TestCase.class);
		Path moduleSearchPath = tc.modules.isEmpty() ? null : ModuleFixtures.materialize(tc.modules);
		try {
			if (tc.version == null || tc.version.contains(jqVersion)) {
				test(tc, jqVersion, moduleSearchPath);
			}
		} finally {
			if (moduleSearchPath != null)
				ModuleFixtures.cleanup(moduleSearchPath);
		}
	}
}
