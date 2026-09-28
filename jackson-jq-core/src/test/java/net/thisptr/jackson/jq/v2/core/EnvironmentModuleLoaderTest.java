package net.thisptr.jackson.jq.v2.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.annotations.ModuleRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the module loaders and registrations retained by an {@link Environment}.
 */
public class EnvironmentModuleLoaderTest {
	private static class SourceModule implements JqModule {
		private final int value;

		SourceModule(int value) {
			this.value = value;
		}

		@Override
		public String getSourceCode() {
			return "def value: " + value + ";";
		}


	}

	@ModuleRegistration(path = "first")
	@ModuleRegistration(path = "second")
	private static final class RegisteredModule extends SourceModule {
		RegisteredModule(int value) {
			super(value);
		}
	}

	private static int run(Environment<JsonNode> env, String query) {
		List<JsonNode> results = new ArrayList<>();
		env.compile(query).apply(NullNode.getInstance(), results::add);
		assertThat(results).hasSize(1);
		return results.get(0).asInt();
	}

	/**
	 * Stands in for a real loader: what matters here is which instances end up in the environment,
	 * in what order, so this one never resolves anything.
	 */
	private static final class StubModuleLoader implements ModuleLoader<JsonNode> {
		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}
	}

	private static EnvironmentBuilder<JsonNode> builder() {
		return EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6);
	}

	@Test
	public void testDefaultEnvironmentHasNoModuleLoaders() {
		assertThat(builder().build().getModuleLoaders()).isEmpty();
	}

	@Test
	public void testAddedLoadersRetainTheirOrder() {
		ModuleLoader<JsonNode> first = new StubModuleLoader();
		ModuleLoader<JsonNode> second = new StubModuleLoader();

		assertThat(builder().addModuleLoader(first).addModuleLoader(second).build().getModuleLoaders())
				.containsExactly(first, second);
	}

	@Test
	public void testClearModuleLoadersTakesOverTheOrder() {
		ModuleLoader<JsonNode> first = new StubModuleLoader();
		ModuleLoader<JsonNode> second = new StubModuleLoader();

		assertThat(builder().clearModuleLoaders().addModuleLoader(first).addModuleLoader(second).build().getModuleLoaders())
				.containsExactly(first, second);
	}

	@Test
	public void testEnvironmentWithNoModuleLoadersCannotImport() {
		Environment<JsonNode> env = builder().clearModuleLoaders().build();

		assertThat(env.getModuleLoaders()).isEmpty();
		assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::bar"))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
	}

	/**
	 * An explicitly added classpath loader uses the class loader it was given. Builtins remain
	 * available even when that loader discovers no modules.
	 */
	@Test
	public void testExplicitClassLoaderReachesOnlyTheModuleLoader() throws Exception {
		try (URLClassLoader nothingRegistered = new URLClassLoader(new URL[0], null)) {
			Environment<JsonNode> env = builder().addModuleLoader(new ClassPathModuleLoader<>(nothingRegistered)).build();

			assertThat(env.getModuleLoaders()).hasSize(1);
			assertThatCode(() -> env.compile("not")).doesNotThrowAnyException();
			assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::bar"))
					.isInstanceOf(ModuleNotFoundException.class);
		}
	}

	@Test
	public void testModuleLoadersAreNotModifiableThroughTheEnvironment() {
		assertThatThrownBy(() -> builder().build().getModuleLoaders().add(new StubModuleLoader()))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void testAvailableModuleUsesEveryRegistrationPath() {
		RegisteredModule module = new RegisteredModule(7);
		Environment<JsonNode> env = builder().registerModule(module).clearModuleLoaders().build();

		assertThat(env.getModuleLoaders()).isEmpty();
		assertThat(env.getRegisteredModules()).containsEntry("first", module).containsEntry("second", module);
		assertThat(run(env, "import \"first\" as m; m::value")).isEqualTo(7);
		assertThat(run(env, "include \"second\"; value")).isEqualTo(7);
	}

	@Test
	public void testOverrideReplacesRegistrationPathsAndWorksWithoutAnnotation() {
		Environment<JsonNode> env = builder().clearModuleLoaders()
				.registerModule("custom", new RegisteredModule(8))
				.registerModule("plain", new SourceModule(9))
				.build();

		assertThat(run(env, "import \"custom\" as m; m::value")).isEqualTo(8);
		assertThat(run(env, "import \"plain\" as m; m::value")).isEqualTo(9);
		assertThatThrownBy(() -> env.compile("import \"first\" as m; m::value"))
				.isInstanceOf(ModuleNotFoundException.class);
	}

	@Test
	public void testAvailableModulePrecedesLoadersAndLatestRegistrationWins() {
		ModuleLoader<JsonNode> loader = new ModuleLoader<>() {
			@Override
			public Module loadModule(String path, Maybe<JsonNode> metadata) {
				return new SourceModule(1);
			}

			@Override
			public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
				throw new ModuleNotFoundException(path);
			}
		};
		Environment<JsonNode> env = builder().clearModuleLoaders()
				.addModuleLoader(loader)
				.registerModule("chosen", new SourceModule(2))
				.registerModule("chosen", new SourceModule(3))
				.build();

		assertThat(run(env, "import \"chosen\" as m; m::value")).isEqualTo(3);
		assertThat(run(env, "import \"other\" as m; m::value")).isEqualTo(1);
	}

	@Test
	public void testBuiltEnvironmentKeepsItsAvailableModuleSnapshot() {
		EnvironmentBuilder<JsonNode> builder = builder().clearModuleLoaders()
				.registerModule("chosen", new SourceModule(1));
		Environment<JsonNode> first = builder.build();
		builder.registerModule("chosen", new SourceModule(2));
		Environment<JsonNode> second = builder.build();

		assertThat(first.getRegisteredModules().get("chosen")).isNotSameAs(second.getRegisteredModules().get("chosen"));
		assertThat(run(first, "import \"chosen\" as m; m::value")).isEqualTo(1);
		assertThat(run(second, "import \"chosen\" as m; m::value")).isEqualTo(2);
	}

	@Test
	public void testRegisteredModulesAreNotModifiableThroughTheEnvironment() {
		Environment<JsonNode> env = builder().registerModule("chosen", new SourceModule(1)).build();

		assertThat(env.getModuleLoaders()).isEmpty();
		assertThatThrownBy(() -> env.getRegisteredModules().put("other", new SourceModule(2)))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	public void testUnannotatedModuleRequiresPathOverride() {
		assertThatThrownBy(() -> builder().registerModule(new SourceModule(1)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
