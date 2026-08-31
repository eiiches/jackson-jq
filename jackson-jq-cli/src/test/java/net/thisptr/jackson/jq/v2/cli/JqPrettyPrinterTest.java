package net.thisptr.jackson.jq.v2.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

class JqPrettyPrinterTest {
	private static final String[] PROVIDERS = { "jackson2", "jackson3", "gson", "jakarta" };

	/**
	 * Nesting, empty containers, and characters jq leaves unescaped.
	 */
	private static final String FIXTURE = "{\"a\":[1,2,{\"b\":null}],\"c\":{},\"d\":[],\"e\":\"<&>\"}";

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void printsNestedValuesLikeJq(String provider) {
		assertThat(print(provider, FIXTURE)).isEqualTo(""
				+ "{\n"
				+ "  \"a\": [\n"
				+ "    1,\n"
				+ "    2,\n"
				+ "    {\n"
				+ "      \"b\": null\n"
				+ "    }\n"
				+ "  ],\n"
				+ "  \"c\": {},\n"
				+ "  \"d\": [],\n"
				+ "  \"e\": \"<&>\"\n"
				+ "}");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void keepsEmptyContainersOnOneLine(String provider) {
		assertThat(print(provider, "{}")).isEqualTo("{}");
		assertThat(print(provider, "[]")).isEqualTo("[]");
		assertThat(print(provider, "[[],{},1]")).isEqualTo(""
				+ "[\n"
				+ "  [],\n"
				+ "  {},\n"
				+ "  1\n"
				+ "]");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void printsScalarsExactlyAsFormatDoes(String provider) {
		for (String json : new String[] { "1", "\"x\"", "null", "true", "1.5", "\"日本語\"", "\"<>&'\\\"\"" })
			assertScalarMatchesFormat(Main.resolveProvider(provider), json);
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void keepsJqNumberFormattingOfParsedNumbers(String provider) {
		// Scalars go through the provider's own format(), so indenting cannot change a number.
		assertThat(print(provider, "{\"exp\":1e10,\"whole\":1.0}")).isEqualTo(""
				+ "{\n"
				+ "  \"exp\": 10000000000,\n"
				+ "  \"whole\": 1\n"
				+ "}");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void keepsJqNumberFormattingOfNonFiniteDoubles(String provider) {
		assertThat(printNonFinite(Main.resolveProvider(provider))).isEqualTo(""
				+ "{\n"
				+ "  \"nan\": null,\n"
				+ "  \"inf\": 1.7976931348623157e+308\n"
				+ "}");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "gson", "jakarta" })
	void honoursTheGivenIndent(String provider) {
		assertThat(printWith(Main.resolveProvider(provider), "{\"a\":[1]}", "\t")).isEqualTo(""
				+ "{\n"
				+ "\t\"a\": [\n"
				+ "\t\t1\n"
				+ "\t]\n"
				+ "}");
	}

	@Test
	void allProvidersAgree() {
		List<String> rendered = new ArrayList<>();
		for (String provider : PROVIDERS)
			rendered.add(print(provider, FIXTURE));
		assertThat(rendered).containsOnly(rendered.get(0));
	}

	private static String print(String provider, String json) {
		return printWith(Main.resolveProvider(provider), json, "  ");
	}

	private static <N> String printWith(JsonProvider<N> provider, String json, String indent) {
		return JqPrettyPrinter.print(provider, provider.parse(json), indent);
	}

	private static <N> void assertScalarMatchesFormat(JsonProvider<N> provider, String json) {
		N node = provider.parse(json);
		assertThat(JqPrettyPrinter.print(provider, node, "  ")).isEqualTo(provider.format(node));
	}

	private static <N> String printNonFinite(JsonProvider<N> provider) {
		Map<String, N> fields = new LinkedHashMap<>();
		fields.put("nan", provider.createNumber(Double.NaN));
		fields.put("inf", provider.createNumber(Double.POSITIVE_INFINITY));
		return JqPrettyPrinter.print(provider, provider.createObject(fields), "  ");
	}
}
