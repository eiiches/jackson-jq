package net.thisptr.jackson.jq.v2.core.module.loaders;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the chain tells "this loader doesn't have it" apart from "this loader had it and failed":
 * only a {@link ModuleNotFoundException} moves on to the next loader.
 */
public class ChainedModuleLoaderTest {

	/**
	 * Has nothing, like a search path the module simply isn't on.
	 */
	private static final class MissingModuleLoader implements ModuleLoader<JsonNode> {
		@Override
		public Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}

		@Override
		public JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			throw new ModuleNotFoundException(path);
		}
	}

	/**
	 * Resolved the path, then failed on it -- a module file with a syntax error, say.
	 */
	private static final class FailingModuleLoader implements ModuleLoader<JsonNode> {
		@Override
		public Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			throw new JsonQueryException("failed to load module " + path + ": boom");
		}

		@Override
		public JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			throw new JsonQueryException("failed to load data " + path + ": boom");
		}
	}

	/**
	 * Answers every path with the same module and the same data.
	 */
	private static final class FixedModuleLoader implements ModuleLoader<JsonNode> {
		private final Module module;
		private final JsonNode data;

		FixedModuleLoader(Module module, JsonNode data) {
			this.module = module;
			this.data = data;
		}

		@Override
		public Module loadModule(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			return module;
		}

		@Override
		public JsonNode loadData(@Nullable Module caller, String path, Maybe<JsonNode> metadata) {
			return data;
		}
	}

	private static final Module MODULE = new EnvironmentBuilder<JsonNode>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
			.build()
			.compileModule("def one: 1;");
	private static final JsonNode DATA = IntNode.valueOf(7);
	private static final Maybe<JsonNode> NO_METADATA = Maybe.absent();

	@Test
	public void testMissLoaderFallsThroughToNextLoader() throws Exception {
		ModuleLoader<JsonNode> chain = new ChainedModuleLoader<>(new MissingModuleLoader(), new FixedModuleLoader(MODULE, DATA));

		assertThat(chain.loadModule(null, "foo", NO_METADATA)).isSameAs(MODULE);
		assertThat(chain.loadData(null, "foo", NO_METADATA)).isSameAs(DATA);
	}

	@Test
	public void testFailingLoaderAbortsChain() throws Exception {
		ModuleLoader<JsonNode> chain = new ChainedModuleLoader<>(new FailingModuleLoader(), new FixedModuleLoader(MODULE, DATA));

		assertThatThrownBy(() -> chain.loadModule(null, "foo", NO_METADATA))
				.isInstanceOf(JsonQueryException.class)
				.isNotInstanceOf(ModuleNotFoundException.class)
				.hasMessage("failed to load module foo: boom");
		assertThatThrownBy(() -> chain.loadData(null, "foo", NO_METADATA))
				.isInstanceOf(JsonQueryException.class)
				.isNotInstanceOf(ModuleNotFoundException.class)
				.hasMessage("failed to load data foo: boom");
	}

	@Test
	public void testAllLoadersMissing() throws Exception {
		ModuleLoader<JsonNode> chain = new ChainedModuleLoader<>(new MissingModuleLoader(), new MissingModuleLoader());

		assertThatThrownBy(() -> chain.loadModule(null, "foo", NO_METADATA))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
		assertThatThrownBy(() -> chain.loadData(null, "foo", NO_METADATA))
				.isInstanceOf(ModuleNotFoundException.class)
				.hasMessage("module not found: foo");
	}

	@Test
	public void testImportThroughChainResolvesFromSecondLoader() throws Exception {
		ModuleLoader<JsonNode> chain = new ChainedModuleLoader<>(new MissingModuleLoader(), new FixedModuleLoader(MODULE, DATA));

		JsonQuery<JsonNode> expr = new EnvironmentBuilder<JsonNode>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.setModuleLoader(chain)
				.build()
				.compile("import \"foo\" as foo; import \"bar\" as $bar; [foo::one, $bar::bar]");

		List<JsonNode> actual = new ArrayList<>();
		expr.apply(NullNode.getInstance(), actual::add);

		// Compared as text: the node classes the compiler produces are not what this test is about.
		assertThat(actual).hasSize(1);
		assertThat(actual.get(0).toString()).isEqualTo("[1,7]");
	}
}
