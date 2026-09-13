package net.thisptr.jackson.jq.v2.core;

import java.net.URL;
import java.net.URLClassLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what an {@link Environment} ends up with: the loader {@link EnvironmentBuilder#withDefaultLoaders}
 * installs, and how {@link EnvironmentBuilder#clearModuleLoaders()} and
 * {@link EnvironmentBuilder#addModuleLoader} change it.
 */
public class EnvironmentModuleLoaderTest {

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
	public void testDefaultEnvironmentHasTheClassPathLoader() throws Exception {
		assertThat(builder().build().getModuleLoaders()).hasSize(1);
	}

	@Test
	public void testAddedLoadersFollowTheDefaultInOrder() throws Exception {
		ModuleLoader<JsonNode> first = new StubModuleLoader();
		ModuleLoader<JsonNode> second = new StubModuleLoader();

		assertThat(builder().addModuleLoader(first).addModuleLoader(second).build().getModuleLoaders())
				.hasSize(3)
				.endsWith(first, second);
	}

	@Test
	public void testClearModuleLoadersTakesOverTheOrder() throws Exception {
		ModuleLoader<JsonNode> first = new StubModuleLoader();
		ModuleLoader<JsonNode> second = new StubModuleLoader();

		assertThat(builder().clearModuleLoaders().addModuleLoader(first).addModuleLoader(second).build().getModuleLoaders())
				.containsExactly(first, second);
	}

	@Test
	public void testEnvironmentWithNoModuleLoadersCannotImport() throws Exception {
		Environment<JsonNode> env = builder().clearModuleLoaders().build();

		assertThat(env.getModuleLoaders()).isEmpty();
		assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::bar"))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
	}

	/**
	 * The class loader the overload names has to reach both default loaders, not just the module one:
	 * a loader that sees no providers leaves the environment without even the jq builtins.
	 */
	@Test
	public void testExplicitClassLoaderReachesBothDefaultLoaders() throws Exception {
		try (URLClassLoader nothingRegistered = new URLClassLoader(new URL[0], null)) {
			Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6, nothingRegistered).build();

			assertThat(env.getModuleLoaders()).hasSize(1);
			assertThatThrownBy(() -> env.compile("not"))
					.isInstanceOf(JsonQueryException.class)
					.hasMessageContaining("not/0");
			assertThatThrownBy(() -> env.compile("import \"foo\" as foo; foo::bar"))
					.isInstanceOf(ModuleNotFoundException.class);
		}
	}

	@Test
	public void testModuleLoadersAreNotModifiableThroughTheEnvironment() throws Exception {
		assertThatThrownBy(() -> builder().build().getModuleLoaders().add(new StubModuleLoader()))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
