package examples;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.ModuleLoader;
import net.thisptr.jackson.jq.v2.core.module.ModuleNotFoundException;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Serving modules from memory rather than from files.
 * <p>
 * A module loader only has to <em>find</em> jq source. Parsing it, resolving whatever that module
 * imports in turn, compiling it and caching the result are the query engine's job, so a loader can
 * be as small as a switch on the import path.
 */
public class InMemoryJqModuleTest {

	private static final class InMemoryJqModuleLoader implements ModuleLoader<JsonNode> {

		@Override
		public Module loadModule(String path, Maybe<JsonNode> metadata) {
			switch (path) {
				case "math":
					return new InMemoryJqModule(path, "def double: . * 2;");
				case "greeting":
					// A module may import another. The engine resolves that import through this
					// same environment's loaders and compiles "math" first -- nothing here has to.
					return new InMemoryJqModule(path, "import \"math\" as math;"
							+ " def shout($n): \"n=\" + ($n | math::double | tostring) + \"!\";");
				default:
					// "I don't have it", which lets the environment's next loader try. Any other
					// exception would mean "I have it and it is broken", and would stop the search.
					throw new ModuleNotFoundException(path);
			}
		}

		@Override
		public JsonNode loadData(String path, Maybe<JsonNode> metadata) {
			// What an `import "..." as $data;` statement asks for. This loader serves no data.
			throw new ModuleNotFoundException(path);
		}
	}

	private static final class InMemoryJqModule implements JqModule<JsonNode> {
		private final String name;
		private final String source;

		InMemoryJqModule(String name, String source) {
			this.name = name;
			this.source = source;
		}

		@Override
		public String getSourceCode() {
			return source;
		}

		@Override
		public JqModule<JsonNode> relativeImport(String importPath, String searchPath) {
			// `import "x" {search: "./"}` means "next to me", and nothing is next to a module that
			// lives in a map. A loader that reads files answers this one instead.
			throw new ModuleNotFoundException(importPath);
		}

		@Override
		public JsonNode relativeData(String importPath, String searchPath) {
			throw new ModuleNotFoundException(importPath);
		}

		/**
		 * Required: the engine compiles each distinct module once per compilation and reports a
		 * circular import when it re-enters one it is still compiling, and both ask modules whether
		 * they are the same module. Two instances made from the same path are.
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof InMemoryJqModule other && name.equals(other.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

	@Test
	public void loadsModulesFromMemory() {
		Jackson3JsonProvider jsonProvider = Jackson3JsonProvider.getInstance();
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_8_2)
				.addModuleLoader(new InMemoryJqModuleLoader())
				.build();

		JsonQuery<JsonNode> query = environment.compile("import \"greeting\" as greeting; greeting::shout(21)");

		List<JsonNode> output = query.apply(jsonProvider.createNull());
		assertThat(output).containsExactly(StringNode.valueOf("n=42!"));
	}
}
