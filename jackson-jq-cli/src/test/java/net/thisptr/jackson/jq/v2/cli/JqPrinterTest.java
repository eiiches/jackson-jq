package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

class JqPrinterTest {
	private static final String[] PROVIDERS = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" };

	private static final String FIXTURE = "{\"a\":[1,2,{\"b\":null}],\"c\":{},\"d\":[],\"e\":\"<&>\"}";

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void printsNestedValuesInColorMatchingJq(String providerName) {
		JsonProvider<?> provider = Main.resolveProvider(providerName);
		JqColors colors = JqColors.defaultFor(Versions.JQ_1_7);

		String expected = """
				\
				\033[1;39m{\033[0m
				  \033[1;34m"a"\033[0m\033[1;39m:\033[0m \033[1;39m[\033[0m
				    \033[0;39m1\033[0m\033[1;39m,\033[0m
				    \033[0;39m2\033[0m\033[1;39m,\033[0m
				    \033[1;39m{\033[0m
				      \033[1;34m"b"\033[0m\033[1;39m:\033[0m \033[0;90mnull\033[0m
				    \033[1;39m}\033[0m
				  \033[1;39m]\033[0m\033[1;39m,\033[0m
				  \033[1;34m"c"\033[0m\033[1;39m:\033[0m \033[1;39m{}\033[0m\033[1;39m,\033[0m
				  \033[1;34m"d"\033[0m\033[1;39m:\033[0m \033[1;39m[]\033[0m\033[1;39m,\033[0m
				  \033[1;34m"e"\033[0m\033[1;39m:\033[0m \033[0;32m"<&>"\033[0m
				\033[1;39m}\033[0m""";

		assertThat(printPretty(provider, FIXTURE, colors)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void printsCompactInColorMatchingJq(String providerName) {
		JsonProvider<?> provider = Main.resolveProvider(providerName);
		JqColors colors = JqColors.defaultFor(Versions.JQ_1_7);

		String expected = "\033[1;39m{\033[0m"
				+ "\033[1;34m\"a\"\033[0m\033[1;39m:\033[0m"
				+ "\033[1;39m[\033[0m\033[0;39m1\033[0m\033[1;39m,\033[0m\033[0;39m2\033[0m\033[1;39m,\033[0m"
				+ "\033[1;39m{\033[0m\033[1;34m\"b\"\033[0m\033[1;39m:\033[0m\033[0;90mnull\033[0m\033[1;39m}\033[0m"
				+ "\033[1;39m]\033[0m\033[1;39m,\033[0m"
				+ "\033[1;34m\"c\"\033[0m\033[1;39m:\033[0m\033[1;39m{}\033[0m\033[1;39m,\033[0m"
				+ "\033[1;34m\"d\"\033[0m\033[1;39m:\033[0m\033[1;39m[]\033[0m\033[1;39m,\033[0m"
				+ "\033[1;34m\"e\"\033[0m\033[1;39m:\033[0m\033[0;32m\"<&>\"\033[0m"
				+ "\033[1;39m}\033[0m";

		assertThat(printCompact(provider, FIXTURE, colors)).isEqualTo(expected);
	}

	@Test
	void allProvidersAgreeOnColoredOutput() {
		JqColors colors = JqColors.defaultFor(Versions.JQ_1_7);
		List<String> rendered = new ArrayList<>();
		for (String provider : PROVIDERS) {
			rendered.add(printPretty(Main.resolveProvider(provider), FIXTURE, colors));
		}
		assertThat(rendered).containsOnly(rendered.get(0));
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void keepsEmptyContainersOnOneLineWhenColored(String providerName) {
		JsonProvider<?> provider = Main.resolveProvider(providerName);
		JqColors colors = JqColors.defaultFor(Versions.JQ_1_7);

		assertThat(printPretty(provider, "{}", colors)).isEqualTo("\033[1;39m{}\033[0m");
		assertThat(printPretty(provider, "[]", colors)).isEqualTo("\033[1;39m[]\033[0m");
		assertThat(printCompact(provider, "{}", colors)).isEqualTo("\033[1;39m{}\033[0m");
		assertThat(printCompact(provider, "[]", colors)).isEqualTo("\033[1;39m[]\033[0m");
	}

	@Test
	void honorsJq16DefaultNullColor() {
		Jackson3JsonProvider provider = Jackson3JsonProvider.getInstance();
		JqColors colors16 = JqColors.defaultFor(Versions.JQ_1_6);
		JqColors colors17 = JqColors.defaultFor(Versions.JQ_1_7);

		assertThat(printCompact(provider, "null", colors16)).isEqualTo("\033[1;30mnull\033[0m");
		assertThat(printCompact(provider, "null", colors17)).isEqualTo("\033[0;90mnull\033[0m");
	}

	@Test
	void appliesCustomJqColorsFromEnvironment() {
		Jackson3JsonProvider provider = Jackson3JsonProvider.getInstance();
		Map<String, String> env = Collections.singletonMap("JQ_COLORS", "1;31:1;32:1;33:1;34:1;35:1;36:1;37:0;31");
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		JqColors colors = JqColors.fromEnvironment(Versions.JQ_1_7, env, new PrintStream(err));

		assertThat(err.toByteArray()).isEmpty();
		assertThat(printCompact(provider, "null", colors)).isEqualTo("\033[1;31mnull\033[0m");
		assertThat(printCompact(provider, "false", colors)).isEqualTo("\033[1;32mfalse\033[0m");
		assertThat(printCompact(provider, "true", colors)).isEqualTo("\033[1;33mtrue\033[0m");
		assertThat(printCompact(provider, "123", colors)).isEqualTo("\033[1;34m123\033[0m");
		assertThat(printCompact(provider, "\"hi\"", colors)).isEqualTo("\033[1;35m\"hi\"\033[0m");
		assertThat(printCompact(provider, "[]", colors)).isEqualTo("\033[1;36m[]\033[0m");
		assertThat(printCompact(provider, "{}", colors)).isEqualTo("\033[1;37m{}\033[0m");
		assertThat(printCompact(provider, "{\"k\":1}", colors))
				.isEqualTo("\033[1;37m{\033[0m\033[0;31m\"k\"\033[0m\033[1;37m:\033[0m\033[1;34m1\033[0m\033[1;37m}\033[0m");
	}

	@Test
	void fillsMissingJqColorsFromDefaults() {
		Jackson3JsonProvider provider = Jackson3JsonProvider.getInstance();
		Map<String, String> env = Collections.singletonMap("JQ_COLORS", "1;31:1;32");
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		JqColors colors = JqColors.fromEnvironment(Versions.JQ_1_7, env, new PrintStream(err));

		assertThat(err.toByteArray()).isEmpty();
		assertThat(printCompact(provider, "null", colors)).isEqualTo("\033[1;31mnull\033[0m");
		assertThat(printCompact(provider, "false", colors)).isEqualTo("\033[1;32mfalse\033[0m");
		// Unspecified slots take defaults
		assertThat(printCompact(provider, "true", colors)).isEqualTo("\033[0;39mtrue\033[0m");
		assertThat(printCompact(provider, "123", colors)).isEqualTo("\033[0;39m123\033[0m");
	}

	@Test
	void warnsAndFallsBackOnInvalidJqColors() {
		Jackson3JsonProvider provider = Jackson3JsonProvider.getInstance();
		Map<String, String> env = Collections.singletonMap("JQ_COLORS", "invalid_color_spec");
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		JqColors colors = JqColors.fromEnvironment(Versions.JQ_1_7, env, new PrintStream(err));

		assertThat(err.toString(StandardCharsets.UTF_8)).isEqualTo("Failed to set $JQ_COLORS\n");
		assertThat(printCompact(provider, "null", colors)).isEqualTo("\033[0;90mnull\033[0m");
	}

	@Test
	void printsMonochromeWhenColorsIsNull() {
		Jackson3JsonProvider provider = Jackson3JsonProvider.getInstance();
		assertThat(printPretty(provider, "{\"a\":[1,null]}", null)).isEqualTo("""
				\
				{
				  "a": [
				    1,
				    null
				  ]
				}""");
		assertThat(printCompact(provider, "{\"a\":[1,null]}", null)).isEqualTo("{\"a\":[1,null]}");
	}

	private static <N> String printPretty(JsonProvider<N> provider, String json, @Nullable JqColors colors) {
		return JqPrinter.print(provider, provider.parse(json), "  ", colors);
	}

	private static <N> String printCompact(JsonProvider<N> provider, String json, @Nullable JqColors colors) {
		return JqPrinter.print(provider, provider.parse(json), null, colors);
	}
}
