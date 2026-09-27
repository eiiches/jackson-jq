package examples;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * jq's regular-expression functions -- {@code test}, {@code match}, {@code capture}, {@code scan},
 * {@code split/2}, {@code splits}, {@code sub}, {@code gsub} -- are not builtins. They ship in
 * jackson-jq-ext-module-joni, and nothing installs them implicitly. An environment may include
 * them, bind an alias, register a query import path, or search ServiceLoader modules explicitly.
 */
public class JoniRegexTest {

	/**
	 * {@code includeModule} is for an application that wants the regex functions available to every
	 * query it compiles, under their bare names, with no directive in the query text.
	 */
	@Test
	public void includedModuleMakesRegexFunctionsCallableByBareName() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.includeModule(new JoniRegexModule())
				.build();

		JsonQuery<JsonNode> query = environment.compile("test(\"^a\")");

		List<JsonNode> output = query.apply(StringNode.valueOf("abc"));
		assertThat(output).containsExactly(BooleanNode.TRUE);
	}

	/**
	 * {@code importModule} is the middle ground: the environment gives the module a namespace, and every
	 * query it compiles can call {@code re::func(...)} without an {@code import} directive of its own.
	 * Unlike {@code includeModule}, nothing becomes callable by its bare name.
	 */
	@Test
	public void importedModuleIsCallableUnderTheNameTheEnvironmentGivesIt() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.importModule(new JoniRegexModule(), "re")
				.build();

		JsonQuery<JsonNode> query = environment.compile("re::scan(\"[0-9]\")");

		List<JsonNode> output = query.apply(StringNode.valueOf("a1b2"));
		assertThat(output).containsExactly(StringNode.valueOf("1"), StringNode.valueOf("2"));
	}

	/**
	 * Registering a module lets a query choose its namespace with an {@code import} directive.
	 */
	@Test
	public void queryCanImportAnExplicitlyRegisteredModule() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.registerModule(new JoniRegexModule())
				.build();

		JsonQuery<JsonNode> query = environment.compile("import \"jackson-jq/joni\" as re; re::gsub(\"\\\\s+\"; \"-\")");

		List<JsonNode> output = query.apply(StringNode.valueOf("a b  c"));
		assertThat(output).containsExactly(StringNode.valueOf("a-b-c"));
	}

	@Test
	public void queryCanImportAServiceLoadedModule() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2)
				.addModuleLoader(new ClassPathModuleLoader<>(JoniRegexTest.class.getClassLoader()))
				.build();

		JsonQuery<JsonNode> query = environment.compile("import \"jackson-jq/joni\" as re; re::gsub(\"\\\\s+\"; \"-\")");

		List<JsonNode> output = query.apply(StringNode.valueOf("a b  c"));
		assertThat(output).containsExactly(StringNode.valueOf("a-b-c"));
	}
}
