package net.thisptr.jackson.jq.v2.ext.joni;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JoniModuleTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final String IMPORT = "import \"jackson-jq/joni\" as re; ";

	@Test
	public void exposesThePublicRegexFunctions() {
		assertThat(run("\"abc\" | re::test(\"^a\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"abc\" | re::test([\"^a\", \"\"])")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"abc\" | re::match(\"b\"; \"\") | [.offset, .length, .string]")).singleElement().satisfies(result ->
				assertThat(JSON_PROVIDER.format(result)).isEqualTo("[1,1,\"b\"]"));
		assertThat(run("\"abc\" | re::match([\"b\", \"\"]) | .string")).extracting(JSON_PROVIDER::getString).containsExactly("b");
		assertThat(run("\"abc\" | re::capture(\"(?<value>b)\"; \"\") | .value")).extracting(JSON_PROVIDER::getString).containsExactly("b");
		assertThat(run("\"abc\" | re::capture([\"(?<value>b)\", \"\"]) | .value")).extracting(JSON_PROVIDER::getString).containsExactly("b");
		assertThat(run("\"a1b2\" | re::scan(\"\\\\d\")")).extracting(JSON_PROVIDER::getString).containsExactly("1", "2");
		assertThat(run("\"a1b2\" | re::scan(\"\\\\d\"; \"\")")).extracting(JSON_PROVIDER::getString).containsExactly("1", "2");
		assertThat(run("\"a,b;c\" | re::splits(\"[,;]\")")).extracting(JSON_PROVIDER::getString).containsExactly("a", "b", "c");
		assertThat(run("\"a,b;c\" | re::splits(\"[,;]\"; \"\")")).extracting(JSON_PROVIDER::getString).containsExactly("a", "b", "c");
		assertThat(run("\"a,b;c\" | re::split(\"[,;]\"; \"\") | join(\"|\")")).extracting(JSON_PROVIDER::getString).containsExactly("a|b|c");
		assertThat(run("\"aba\" | re::sub(\"a\"; \"x\")")).extracting(JSON_PROVIDER::getString).containsExactly("xba");
		assertThat(run("\"aba\" | re::sub(\"a\"; \"x\"; \"g\")")).extracting(JSON_PROVIDER::getString).containsExactly("xbx");
		assertThat(run("\"aba\" | re::gsub(\"a\"; \"x\")")).extracting(JSON_PROVIDER::getString).containsExactly("xbx");
		assertThat(run("\"aba\" | re::gsub(\"a\"; \"x\"; \"i\")")).extracting(JSON_PROVIDER::getString).containsExactly("xbx");
	}

	@Test
	public void includeExposesRegexFunctionsWithoutAQualifier() {
		assertThat(runQuery("include \"jackson-jq/joni\"; \"abc\" | test(\"^a\")", RuntimeOptions.newBuilder().build()))
				.containsExactly(JSON_PROVIDER.createBoolean(true));
	}

	/**
	 * The route an application takes when it wants the regex functions available to every query it
	 * compiles, without a directive in the query text.
	 */
	@Test
	public void includeModuleExposesRegexFunctionsWithoutAQualifier() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).registerModule(new JoniRegexModule())
				.includeModule(new JoniRegexModule())
				.build();
		assertThat(environment.compile("\"abc\" | test(\"^a\")").apply(JSON_PROVIDER.createNull()))
				.containsExactly(JSON_PROVIDER.createBoolean(true));
	}

	/**
	 * Oniguruma is what separates this engine from the re2 one: it reads backreferences and look-around,
	 * its character classes are Unicode-aware, and it accepts jq's whole flag set rather than the subset
	 * re2 can express.
	 */
	@Test
	public void usesOnigurumaSyntaxAndFlagSemantics() {
		assertThat(run("\"abb\" | re::test(\"(?<v>b)\\\\k<v>\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"a\" | re::test(\"(?=a)\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"١\" | re::test(\"\\\\d\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"a\\nb\" | re::test(\"a.b\"; \"m\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"aa\" | re::match(\"a|aa\"; \"l\") | .string")).extracting(JSON_PROVIDER::getString).containsExactly("aa");
		assertThat(run("\"a\" | re::test(\"a\"; \"n\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"a\" | re::test(\"a\"; \"x\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThatThrownBy(() -> run("\"a\" | re::test(\"a\"; \"z\")"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("z is not a valid modifier string");
	}

	/**
	 * The functions arrive only where something asks for them. Nothing on the classpath installs them.
	 */
	@Test
	public void doesNotRegisterRegexFunctionsGlobally() {
		Environment<JsonNode> environment = environment();
		assertThatThrownBy(() -> environment.compile("\"a\" | test(\"a\")")).isInstanceOf(JsonQueryException.class);
	}

	private static List<JsonNode> run(String expression) throws JsonQueryException {
		return runQuery(IMPORT + expression, RuntimeOptions.newBuilder().build());
	}

	private static List<JsonNode> runQuery(String expression, RuntimeOptions options) throws JsonQueryException {
		Environment<JsonNode> environment = environment();
		JsonQuery<JsonNode> query = environment.compile(expression).withRuntimeOptions(options);
		return query.apply(JSON_PROVIDER.createNull());
	}

	private static Environment<JsonNode> environment() {
		return EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).registerModule(new JoniRegexModule())
				.clearModuleLoaders()
				.registerModule(new JoniRegexModule())
				.build();
	}
}
