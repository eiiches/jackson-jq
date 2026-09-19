package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.ArrayList;
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

public class Re2ModuleTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();
	private static final String IMPORT = "import \"jackson-jq/re2\" as re; ";

	@Test
	public void exposesThePublicRegexFunctions() throws Exception {
		assertThat(run("\"abc\" | re::test(\"^a\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"abc\" | re::test([\"^a\", \"\"])")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"abc\" | re::match(\"b\"; \"\") | [.offset, .length, .string]")).singleElement().satisfies(result ->
				assertThat(JSON_PROVIDER.format(result)).isEqualTo("[1,1,\"b\"]"));
		assertThat(run("\"abc\" | re::match([\"b\", \"\"]) | .string")).extracting(JSON_PROVIDER::getString).containsExactly("b");
		assertThat(run("\"abc\" | re::capture(\"(?<value>b)\"; \"\") | .value")).extracting(JSON_PROVIDER::getString).containsExactly("b");
		assertThat(run("\"abc\" | re::capture([\"(?P<value>b)\", \"\"]) | .value")).extracting(JSON_PROVIDER::getString).containsExactly("b");
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
	public void includeExposesRegexFunctionsWithoutAQualifier() throws Exception {
		assertThat(runQuery("include \"jackson-jq/re2\"; \"abc\" | test(\"^a\")", RuntimeOptions.newBuilder().build()))
				.containsExactly(JSON_PROVIDER.createBoolean(true));
	}

	@Test
	public void usesRe2SyntaxAndFlagSemantics() throws Exception {
		assertThat(run("\"a\\nb\" | re::test(\"a.b\"; \"m\")")).containsExactly(JSON_PROVIDER.createBoolean(true));
		assertThat(run("\"aa\" | re::match(\"a|aa\"; \"l\") | .string")).extracting(JSON_PROVIDER::getString).containsExactly("aa");
		assertThat(run("\"١\" | re::test(\"\\\\d\")")).containsExactly(JSON_PROVIDER.createBoolean(false));
		assertThatThrownBy(() -> run("\"a\" | re::test(\"(?=a)\")")).isInstanceOf(RuntimeException.class);
		assertThatThrownBy(() -> run("\"a\" | re::test(\"a\"; \"n\")")).isInstanceOf(JsonQueryException.class);
		assertThatThrownBy(() -> run("\"a\" | re::test(\"a\"; \"x\")")).isInstanceOf(JsonQueryException.class);
	}

	@Test
	public void reportsCodePointOffsetsAndAdvancesZeroWidthMatchesByCodePoint() throws Exception {
		assertThat(run("\"a😀b\" | re::match(\"😀\"; \"\") | [.offset, .length]")).singleElement().satisfies(result ->
				assertThat(JSON_PROVIDER.format(result)).isEqualTo("[1,1]"));
		assertThat(run("\"a😀b\" | re::gsub(\"\"; \"X\")")).extracting(JSON_PROVIDER::getString).containsExactly("XaX😀XbX");
	}

	@Test
	public void preservesReplacementBranchesAndRuntimeLimits() throws Exception {
		assertThat(run("\"aa\" | re::gsub(\"a\"; \"x\", \"y\")")).extracting(JSON_PROVIDER::getString).containsExactly("xx", "yx", "xy", "yy");
		assertThatThrownBy(() -> run("\"aaaa\" | re::gsub(\"a\"; \"xxxxxxxxxx\")", RuntimeOptions.newBuilder().setMaxStringLength(39).build()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 39");
	}

	@Test
	public void doesNotRegisterRegexFunctionsGlobally() {
		Environment<JsonNode> environment = environment();
		assertThatThrownBy(() -> environment.compile("\"a\" | test(\"a\")")).isInstanceOf(JsonQueryException.class);
	}

	private static List<JsonNode> run(String expression) throws JsonQueryException {
		return run(expression, RuntimeOptions.newBuilder().build());
	}

	private static List<JsonNode> run(String expression, RuntimeOptions options) throws JsonQueryException {
		return runQuery(IMPORT + expression, options);
	}

	private static List<JsonNode> runQuery(String expression, RuntimeOptions options) throws JsonQueryException {
		Environment<JsonNode> environment = environment();
		JsonQuery<JsonNode> query = environment.compile(expression).withRuntimeOptions(options);
		List<JsonNode> results = new ArrayList<>();
		query.apply(JSON_PROVIDER.createNull(), results::add);
		return results;
	}

	private static Environment<JsonNode> environment() {
		return EnvironmentBuilder.withDefaultLoaders(JSON_PROVIDER, Versions.JQ_1_8_2).build();
	}
}
