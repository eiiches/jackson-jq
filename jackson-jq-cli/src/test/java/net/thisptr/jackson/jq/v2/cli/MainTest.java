package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import dev.tamboui.backend.jline3.JLineBackend;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MainTest {
	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void evaluatesWithSelectedJsonProvider(String provider) throws Exception {
		assertThat(run("{\"foo\":41}", "--json-provider", provider, "--compact", ".foo + 1"))
				.isEqualTo("42\n");
	}

	@Test
	void defaultsToJackson3() throws Exception {
		assertThat(run("{\"foo\":41}", "--compact", ".foo + 1"))
				.isEqualTo("42\n");
	}

	@Test
	void supportsRawOutputWithSelectedJsonProvider() throws Exception {
		assertThat(run("null", "--json-provider", "gson", "--raw-output", "\"hello\""))
				.isEqualTo("hello\n");
	}

	@Test
	void supportsNullInputWithSelectedJsonProvider() throws Exception {
		assertThat(run("ignored", "--json-provider", "jackson2", "--null-input", "--compact", ". == null"))
				.isEqualTo("true\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void readsMultipleInputDocumentsWithSelectedJsonProvider(String provider) throws Exception {
		assertThat(run("""
				1 2
				{"a":3}
				[4,5] "six" null true\
				""", "--json-provider", provider, "--compact", "."))
				.isEqualTo("1\n2\n{\"a\":3}\n[4,5]\n\"six\"\nnull\ntrue\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void treatsTopLevelArrayAsOneDocument(String provider) throws Exception {
		assertThat(run("[1,2,3]", "--json-provider", provider, "--compact", "length"))
				.isEqualTo("3\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void prettyPrintsLikeJqByDefault(String provider) throws Exception {
		assertThat(run("{\"a\":[1,2,{\"b\":null}],\"c\":{},\"d\":[],\"e\":\"<&>\"}", "--json-provider", provider, "."))
				.isEqualTo("""
						\
						{
						  "a": [
						    1,
						    2,
						    {
						      "b": null
						    }
						  ],
						  "c": {},
						  "d": [],
						  "e": "<&>"
						}
						""");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void prettyPrintsScalarsAndEmptyContainersOnOneLine(String provider) throws Exception {
		assertThat(run("1 \"two\" null true [] {}", "--json-provider", provider, "."))
				.isEqualTo("1\n\"two\"\nnull\ntrue\n[]\n{}\n");
	}

	@Test
	void readsQueryFromFile(@TempDir Path dir) throws Exception {
		Path query = write(dir, "query.jq", ".foo + 1\n");
		assertThat(run("{\"foo\":41}", "--compact", "-f", query.toString()))
				.isEqualTo("42\n");
		assertThat(run("{\"foo\":41}", "--compact", "--from-file", query.toString()))
				.isEqualTo("42\n");
	}

	@Test
	void readsMultiLineQueryWithCommentsFromFile(@TempDir Path dir) throws Exception {
		Path query = write(dir, "query.jq", """
				#!/usr/bin/env jq -f
				# doubles .foo
				.foo
				\t| . * 2
				""");
		assertThat(run("{\"foo\":21}", "--compact", "-f", query.toString()))
				.isEqualTo("42\n");
	}

	@Test
	void readsInputFromFile(@TempDir Path dir) throws Exception {
		Path input = write(dir, "input.json", "{\"foo\":41}");
		assertThat(run("", "--compact", ".foo", input.toString()))
				.isEqualTo("41\n");
	}

	@Test
	void concatenatesInputFilesInOrder(@TempDir Path dir) throws Exception {
		Path first = write(dir, "first.json", "1 2\n");
		Path second = write(dir, "second.json", "{\"a\":3}\n");
		assertThat(run("", "--compact", ".", first.toString(), second.toString()))
				.isEqualTo("1\n2\n{\"a\":3}\n");
	}

	@Test
	void readsStandardInputForDashInputFile(@TempDir Path dir) throws Exception {
		Path input = write(dir, "input.json", "1\n");
		assertThat(run("2", "--compact", ".", input.toString(), "-"))
				.isEqualTo("1\n2\n");
	}

	@Test
	void combinesQueryFileWithInputFiles(@TempDir Path dir) throws Exception {
		Path query = write(dir, "query.jq", ".foo + 1\n");
		Path first = write(dir, "first.json", "{\"foo\":41}");
		Path second = write(dir, "second.json", "{\"foo\":1}");
		assertThat(run("", "--compact", "-f", query.toString(), first.toString(), second.toString()))
				.isEqualTo("42\n2\n");
	}

	@Test
	void ignoresInputFilesWithNullInput(@TempDir Path dir) throws Exception {
		Path input = write(dir, "input.json", "{\"foo\":41}");
		assertThat(run("", "--compact", "--null-input", ".", input.toString()))
				.isEqualTo("null\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void readsEachLineAsStringWithRawInput(String provider) throws Exception {
		assertThat(run("a\nb\nc\n", "--json-provider", provider, "--raw-input", "--compact", "."))
				.isEqualTo("\"a\"\n\"b\"\n\"c\"\n");
	}

	@Test
	void emitsUnterminatedFinalLineWithRawInput() throws Exception {
		assertThat(run("a\nb", "-R", "--compact", "."))
				.isEqualTo("\"a\"\n\"b\"\n");
	}

	@Test
	void keepsCarriageReturnWithRawInput() throws Exception {
		assertThat(run("a\r\nb\r\n", "-R", "--compact", "."))
				.isEqualTo("\"a\\r\"\n\"b\\r\"\n");
	}

	@Test
	void keepsLoneCarriageReturnWithinLineWithRawInput() throws Exception {
		assertThat(run("a\rb\n", "-R", "--compact", "."))
				.isEqualTo("\"a\\rb\"\n");
	}

	@Test
	void emitsEmptyStringForBlankLineWithRawInput() throws Exception {
		assertThat(run("a\n\nb\n", "-R", "--compact", "."))
				.isEqualTo("\"a\"\n\"\"\n\"b\"\n");
	}

	@Test
	void producesNoOutputForEmptyRawInput() throws Exception {
		assertThat(run("", "-R", "--compact", "."))
				.isEmpty();
	}

	@Test
	void decodesRawInputAsUtf8() throws Exception {
		assertThat(run("\u3042\n", "-R", "--compact", "."))
				.isEqualTo("\"\u3042\"\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void collectsAllInputsIntoArrayWithSlurp(String provider) throws Exception {
		assertThat(run("1 2\n[3]", "--json-provider", provider, "--slurp", "--compact", "."))
				.isEqualTo("[1,2,[3]]\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void producesEmptyArrayForEmptySlurpedInput(String provider) throws Exception {
		assertThat(run("", "--json-provider", provider, "-s", "--compact", "."))
				.isEqualTo("[]\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "jackson2", "jackson3", "fastjson2", "gson", "jakarta" })
	void readsWholeInputAsOneStringWithRawInputAndSlurp(String provider) throws Exception {
		assertThat(run("a\nb\n", "--json-provider", provider, "--raw-input", "--slurp", "--compact", "."))
				.isEqualTo("\"a\\nb\\n\"\n");
	}

	@Test
	void keepsEmptyStringForEmptyRawSlurpedInput() throws Exception {
		assertThat(run("", "-R", "-s", "--compact", "."))
				.isEqualTo("\"\"\n");
	}

	@Test
	void prefersNullInputOverSlurpAndRawInput() throws Exception {
		assertThat(run("1 2", "-n", "-s", "--compact", ".")).isEqualTo("null\n");
		assertThat(run("a\nb", "-n", "-R", "--compact", ".")).isEqualTo("null\n");
	}

	@Test
	void acceptsClusteredShortOptions() throws Exception {
		assertThat(run("a\nb\n", "-Rsc", "."))
				.isEqualTo("\"a\\nb\\n\"\n");
	}

	@Test
	void slurpsAcrossInputFiles(@TempDir Path dir) throws Exception {
		Path first = write(dir, "first.json", "1 2\n");
		Path second = write(dir, "second.json", "{\"a\":3}\n");
		assertThat(run("", "--slurp", "--compact", ".", first.toString(), second.toString()))
				.isEqualTo("[1,2,{\"a\":3}]\n");
	}

	@Test
	void joinsPartialLineAcrossInputFilesWithRawInput(@TempDir Path dir) throws Exception {
		Path first = write(dir, "first.txt", "x\ny");
		Path second = write(dir, "second.txt", "z\n");
		assertThat(run("", "--raw-input", "--compact", ".", first.toString(), second.toString()))
				.isEqualTo("\"x\"\n\"yz\"\n");
	}

	@Test
	void readsWholeInputFilesAsOneStringWithRawInputAndSlurp(@TempDir Path dir) throws Exception {
		Path first = write(dir, "first.txt", "x\ny");
		Path second = write(dir, "second.txt", "z\n");
		assertThat(run("", "-R", "-s", "--compact", ".", first.toString(), second.toString()))
				.isEqualTo("\"x\\nyz\\n\"\n");
	}

	// jq itself emits no warnings, so this must stay on stderr: stdout has to remain exactly what
	// jq would print.
	@Test
	void warnsOnStderrAboutACommaOperandOfAPipe() throws Exception {
		assertThat(run("{\"a\":1,\"b\":2}", "--compact", ".a, .b | .")).isEqualTo("1\n2\n");
		assertThat(runStderr("{\"a\":1,\"b\":2}", "--compact", ".a, .b | ."))
				.contains("""
						jq: warning: `,` binds tighter than `|`: write `(.a, .b)` to make the grouping explicit\
						 at line 1, column 1:
						    .a, .b | .
						    ^
						""");
	}

	@Test
	void warnsOnStderrAboutABindingPipeAfterAComma() throws Exception {
		assertThat(run("null", "--compact", "1 + 1, 2 as $a | $a + 1")).isEqualTo("2\n3\n");
		assertThat(runStderr("null", "--compact", "1 + 1, 2 as $a | $a + 1"))
				.contains("""
						jq: warning: `as` binds only `2`: write `(2 as $a | $a + 1)` to make the grouping explicit\
						 at line 1, column 8:
						    1 + 1, 2 as $a | $a + 1
						           ^
						""");
	}

	@Test
	void suppressesWarningsOnRequest() throws Exception {
		assertThat(runStderr("{\"a\":1,\"b\":2}", "--compact", "--no-warnings", ".a, .b | .")).isEmpty();
	}

	@Test
	void optimizesTailCallsUnlessAskedNotTo() throws Exception {
		// 5000 iterations is far past what one Java call per iteration allows, so getting an answer at all is
		// the optimization working. The other half -- that --disable-tco really does bring the stack cost
		// back -- is asserted in the core's TailCallTest instead: how deep a recursion gets before the stack
		// runs out depends on the JVM's stack size, which is no more predictable here than anywhere else.
		String query = "0 | def f: if . < 5000 then . + 1 | f else . end; f";

		assertThat(run("null", "--compact", query)).isEqualTo("5000\n");
		assertThat(runStderr("null", "--compact", "--no-warnings", "--disable-tco", "0 | def f: if . < 8 then . + 1 | f else . end; f")).isEmpty();
		assertThat(run("null", "--compact", "--disable-tco", "0 | def f: if . < 8 then . + 1 | f else . end; f")).isEqualTo("8\n");
	}

	@Test
	void configuresRuntimeLimits() throws Exception {
		RuntimeOptions options = Main.createRuntimeOptions(parseLimits(
				"--max-string-length", "11",
				"--max-binary-length", "12",
				"--max-array-length", "13",
				"--max-object-member-count", "14",
				"--max-user-defined-function-calls", "15",
				"--max-outputs-per-expression", "16"));

		assertThat(options.getMaxStringLength()).isEqualTo(11);
		assertThat(options.getMaxBinaryLength()).isEqualTo(12);
		assertThat(options.getMaxArrayLength()).isEqualTo(13);
		assertThat(options.getMaxObjectMemberCount()).isEqualTo(14);
		assertThat(options.getMaxUserDefinedFunctionCalls()).isEqualTo(15);
		assertThat(options.getMaxOutputsPerExpression()).isEqualTo(16);
	}

	@Test
	void runtimeLimitsDefaultToUnlimited() throws Exception {
		RuntimeOptions options = Main.createRuntimeOptions(parseLimits());

		assertThat(options.getMaxStringLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(options.getMaxBinaryLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(options.getMaxArrayLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(options.getMaxObjectMemberCount()).isEqualTo(Integer.MAX_VALUE);
		assertThat(options.getMaxUserDefinedFunctionCalls()).isEqualTo(Long.MAX_VALUE);
		assertThat(options.getMaxOutputsPerExpression()).isEqualTo(Long.MAX_VALUE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "--max-string-length", "--max-binary-length", "--max-array-length", "--max-object-member-count", "--max-user-defined-function-calls", "--max-outputs-per-expression" })
	void rejectsInvalidRuntimeLimits(String option) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.createRuntimeOptions(parseLimits(option, "-1")))
				.withMessage("invalid " + option + ": -1 (expected a non-negative integer)");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.createRuntimeOptions(parseLimits(option, "many")))
				.withMessage("invalid " + option + ": many (expected a non-negative integer)");
	}

	@Test
	void saysNothingWhenTheGroupingIsExplicit() throws Exception {
		assertThat(runStderr("{\"a\":1,\"b\":2}", "--compact", "(.a, .b) | .")).isEmpty();
	}

	@Test
	void checksTypesWithWarningsUnlessToldOtherwise() throws Exception {
		assertThat(Main.createTypeCheckMode(parseLimits())).isEqualTo(TypeCheckMode.WARN);
		assertThat(Main.createTypeCheckMode(parseLimits("--type-check", "off"))).isEqualTo(TypeCheckMode.OFF);
		assertThat(Main.createTypeCheckMode(parseLimits("--type-check", "warn"))).isEqualTo(TypeCheckMode.WARN);
		assertThat(Main.createTypeCheckMode(parseLimits("--type-check", "strict"))).isEqualTo(TypeCheckMode.STRICT);
		assertThat(Main.createTypeCheckMode(parseLimits("--type-check", "STRICT"))).isEqualTo(TypeCheckMode.STRICT);
	}

	@Test
	void readsTheInputTypeInTheNotationOfTheTypePackage() throws Exception {
		assertThat(Main.createInputType(parseLimits())).isSameAs(AnyType.getInstance());
		assertThat(Main.createInputType(parseLimits("--input-type", "STRING"))).isSameAs(StringType.getInstance());
		assertThat(Main.createInputType(parseLimits("--input-type", "[*:{name:STRING}]")))
				.isEqualTo(ArrayType.of(ObjectType.of("name", StringType.getInstance())));
	}

	@Test
	void rejectsAnUnparseableInputType() throws Exception {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.createInputType(parseLimits("--input-type", "[*:")))
				.withMessageStartingWith("invalid --input-type: ")
				.withMessageContaining("[*:");
	}

	@Test
	void readsTheOutputTypeInTheNotationOfTheTypePackage() throws Exception {
		assertThat(Main.createOutputType(parseLimits())).isSameAs(AnyType.getInstance());
		assertThat(Main.createOutputType(parseLimits("--output-type", "STRING"))).isSameAs(StringType.getInstance());
		assertThat(Main.createOutputType(parseLimits("--output-type", "[*:{name:STRING}]")))
				.isEqualTo(ArrayType.of(ObjectType.of("name", StringType.getInstance())));
	}

	@Test
	void rejectsAnUnparseableOutputType() throws Exception {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.createOutputType(parseLimits("--output-type", "[*:")))
				.withMessageStartingWith("invalid --output-type: ")
				.withMessageContaining("[*:");
	}

	@Test
	void collectsEveryCompileOptionTheCommandLineCarries() throws Exception {
		CompileOptions options = Main.createCompileOptions(parseLimits(
				"--type-check", "strict", "--input-type", "STRING", "--output-type", "INT", "--disable-tco"));

		assertThat(options.getTypeCheckMode()).isEqualTo(TypeCheckMode.STRICT);
		assertThat(options.getInputType()).isSameAs(StringType.getInstance());
		assertThat(options.getOutputType()).isEqualTo(NumericType.of(NumberKind.INT));
		assertThat(options.getOptimizationOptions().getTailCallOptimization()).isFalse();
	}

	// Declaring what the query must produce turns a query jq runs happily into a warning, without
	// changing a byte of what it prints.
	@Test
	void statingTheOutputTypeFaultsAResultThatDoesNotFit() throws Exception {
		assertThat(runStderr("null", "--compact", "1 + 1")).isEmpty();
		assertThat(runStderr("null", "--compact", "--output-type", "STRING", "\"a\"")).isEmpty();
		assertThat(runStderr("null", "--compact", "--output-type", "STRING", "1 + 1"))
				.contains("jq: warning: Output type NUMBER is not assignable to the declared output type STRING");
		assertThat(run("null", "--compact", "--output-type", "STRING", "1 + 1")).isEqualTo("2\n");
	}

	@Test
	void saysNothingAboutTheOutputTypeWhenCheckingIsOff() throws Exception {
		assertThat(runStderr("null", "--compact", "--type-check", "off", "--output-type", "STRING", "1 + 1")).isEmpty();
	}

	@Test
	void rejectsUnknownTypeCheckMode() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.resolveTypeCheckMode("unknown"))
				.withMessage("unknown --type-check: unknown (expected one of: off, warn, strict)");
	}

	// A type warning never stops the query: it goes to stderr and stdout stays what jq would print.
	// `?` makes the query one jq runs happily to no output, so what is asserted is the warning alone.
	@Test
	void warnsOnStderrAboutAnImpossibleIndex() throws Exception {
		assertThat(run("null", "--compact", "1 | .a?")).isEmpty();
		assertThat(runStderr("null", "--compact", "1 | .a?")).contains("jq: warning: Cannot index");
	}

	@Test
	void placesOverloadLocationBeforeAcceptedTypes() throws Exception {
		assertThat(runStderr("null", "--compact", "\"test\" | ltrimstr([1])"))
				.contains("jq: warning: Argument 1 of ltrimstr/1 has type [INT]; expected STRING at line 1, column 19:")
				.contains("\nAccepted types:\n  Input: STRING -> ltrimstr(STRING -> STRING) -> Output: STRING\n");
	}

	// `map` is written in jq, so what fails is a `.[]` inside its body. The caller is told which call of
	// theirs led there instead of being shown an operation their query does not contain.
	@Test
	void tracesACallIntoTheBuiltinWhoseBodyFailed() throws Exception {
		assertThat(runStderr("null", "--compact", "\"test\" | map(\"test\")?"))
				.contains("jq: warning: Cannot iterate over STRING at line 1, column 10:\n  in map/1\n");
	}

	// Without --input-type nothing is known about the input, so the index cannot be faulted. Saying
	// what the input is turns the same query into a warning, which is the point of the option.
	@Test
	void statingTheInputTypeLetsTheCheckerSeeMore() throws Exception {
		assertThat(runStderr("[{\"name\":\"a\"}]", "--compact", ".[].name | .x?")).isEmpty();
		assertThat(runStderr("[{\"name\":\"a\"}]", "--compact", "--input-type", "[*:{name:STRING}]",
				".[].name | .x?")).contains("jq: warning: Cannot index STRING with a string");
		assertThat(run("[{\"name\":\"a\"}]", "--compact", "--input-type", "[*:{name:STRING}]",
				".[].name")).isEqualTo("\"a\"\n");
	}

	@Test
	void saysNothingAboutTypesWhenCheckingIsOff() throws Exception {
		assertThat(runStderr("null", "--compact", "--type-check", "off", "1 | .a?")).isEmpty();
	}

	@Test
	void suppressesTypeWarningsOnRequest() throws Exception {
		assertThat(runStderr("null", "--compact", "--no-warnings", "1 | .a?")).isEmpty();
	}

	// The CLI's own exit path calls System.exit, which would take the test JVM with it, so the half
	// that is testable here is that `strict` really does turn a type problem into a failing compile.
	@Test
	void strictTypeCheckingRejectsTheQuery() throws Exception {
		CompileOptions options = CompileOptions.newBuilder()
				.setTypeCheckMode(Main.createTypeCheckMode(parseLimits("--type-check", "strict")))
				.build();
		Environment<JsonNode> env = Main.createEnvironment(Jackson3JsonProvider.getInstance(), Versions.JQ_1_6);

		assertThatThrownBy(() -> env.compile("1 | .a", options))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("Type checking failed");
		assertThat(env.compile("1 | .a", CompileOptions.newBuilder().build())).isNotNull();
	}

	@Test
	void rejectsUnknownJsonProvider() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.resolveProvider("unknown"))
				.withMessage("unknown --json-provider: unknown (expected one of: jackson2, jackson3, fastjson2, gson, jakarta)");
	}

	@Test
	void colorizesOutputWithShortOption() throws Exception {
		assertThat(run("{\"a\":1}", "-C", "."))
				.isEqualTo("\033[1;39m{\033[0m\n  \033[1;34m\"a\"\033[0m\033[1;39m:\033[0m \033[0;39m1\033[0m\n\033[1;39m}\033[0m\n");
	}

	@Test
	void colorizesOutputWithLongOption() throws Exception {
		assertThat(run("{\"a\":1}", "--color-output", "."))
				.isEqualTo("\033[1;39m{\033[0m\n  \033[1;34m\"a\"\033[0m\033[1;39m:\033[0m \033[0;39m1\033[0m\n\033[1;39m}\033[0m\n");
	}

	@Test
	void disablesColorWithMonochromeOption() throws Exception {
		assertThat(run("{\"a\":1}", "-M", "."))
				.isEqualTo("{\n  \"a\": 1\n}\n");
		assertThat(run("{\"a\":1}", "--monochrome-output", "."))
				.isEqualTo("{\n  \"a\": 1\n}\n");
	}

	@Test
	void monochromeWinsWhenBothColorAndMonochromeSpecified() throws Exception {
		assertThat(run("{\"a\":1}", "-C", "-M", "."))
				.isEqualTo("{\n  \"a\": 1\n}\n");
		assertThat(run("{\"a\":1}", "-M", "-C", "."))
				.isEqualTo("{\n  \"a\": 1\n}\n");
	}

	@Test
	void colorizesCompactOutput() throws Exception {
		assertThat(run("{\"a\":1}", "-c", "-C", "."))
				.isEqualTo("\033[1;39m{\033[0m\033[1;34m\"a\"\033[0m\033[1;39m:\033[0m\033[0;39m1\033[0m\033[1;39m}\033[0m\n");
		assertThat(run("{\"a\":1}", "-cC", "."))
				.isEqualTo("\033[1;39m{\033[0m\033[1;34m\"a\"\033[0m\033[1;39m:\033[0m\033[0;39m1\033[0m\033[1;39m}\033[0m\n");
	}

	@Test
	void handlesRawOutputWithColor() throws Exception {
		assertThat(run("[\"hello\", 123]", "-r", "-C", ".[]"))
				.isEqualTo("hello\n\033[0;39m123\033[0m\n");
	}

	@ParameterizedTest
	@ValueSource(strings = { "-i", "--interactive", "--vim", "--vim=true" })
	void runsPlaygroundWithCustomRunner(String opt) throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder().longOpt("vim").hasArg().get());
		options.addOption(Option.builder("c").longOpt("compact").get());
		CommandLine command = Main.createCommandLineParser().parse(options, new String[] { opt, "-c" });

		ByteArrayOutputStream termOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner("", termOut);
		if (opt.equals("--vim") || opt.equals("--vim=true")) {
			runner.dispatch(KeyEvent.ofChar(':'));
			runner.dispatch(KeyEvent.ofChar('q'));
			runner.dispatch(KeyEvent.ofKey(KeyCode.ENTER));
		} else {
			runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
		}
		runner.dispatch(KeyEvent.ofChar('y'));

		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try {
			System.setIn(new ByteArrayInputStream("{\"x\":123}".getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(out));
			System.setErr(new PrintStream(err));
			Main.run(command, ".", Collections.emptyList(), Versions.JQ_1_6,
					Jackson3JsonProvider.getInstance(),
					RuntimeOptions.newBuilder().build(), warnTypeChecking(), runner, null);
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}

		assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("{\"x\":123}\n");
		assertThat(err.toString(StandardCharsets.UTF_8)).isEqualTo("jackson-jq -c -- '.'\n");
	}

	@ParameterizedTest
	@CsvSource({
			"--interactive, vim, true",
			"--interactive, vi, true",
			"--interactive, /usr/bin/vim, true",
			"--interactive, nano, false",
			"--interactive, nvim, false",
			"'--interactive --vim=auto', vi, true",
			"'--interactive --vim=auto', nano, false",
			"'--interactive --vim=false', vim, false",
			"'--vim=true', nano, true"
	})
	void runsPlaygroundWithAutoVimAndOverrides(String cliArgs, String editor, boolean expectVim) throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder().longOpt("vim").hasArg().get());
		options.addOption(Option.builder("c").longOpt("compact").get());
		String[] args = (cliArgs + " -c").split(" ");
		CommandLine command = Main.createCommandLineParser().parse(options, args);

		ByteArrayOutputStream termOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner("", termOut);
		if (expectVim) {
			runner.dispatch(KeyEvent.ofChar(':'));
			runner.dispatch(KeyEvent.ofChar('q'));
			runner.dispatch(KeyEvent.ofKey(KeyCode.ENTER));
		} else {
			runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
		}
		runner.dispatch(KeyEvent.ofChar('y'));

		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try {
			System.setIn(new ByteArrayInputStream("{\"x\":123}".getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(out));
			System.setErr(new PrintStream(err));
			Main.run(command, ".", Collections.emptyList(), Versions.JQ_1_6,
					Jackson3JsonProvider.getInstance(),
					RuntimeOptions.newBuilder().build(), warnTypeChecking(), runner, editor);
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}

		assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("{\"x\":123}\n");
		assertThat(err.toString(StandardCharsets.UTF_8)).isEqualTo("jackson-jq -c -- '.'\n");
	}

	@Test
	void parsesVimModesWithoutConsumingTheQuery() throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder().longOpt("vim").hasArg().get());

		CommandLine omitted = Main.createCommandLineParser().parse(options, new String[] { ".name" });
		assertThat(omitted.getOptionValue("vim")).isNull();
		assertThat(omitted.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(omitted)).isFalse();

		CommandLine bareVim = Main.createCommandLineParser().parse(options, new String[] { "--vim", ".name" });
		assertThat(bareVim.getOptionValue("vim")).isEqualTo("true");
		assertThat(bareVim.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(bareVim)).isTrue();

		CommandLine explicitVim = Main.createCommandLineParser().parse(options, new String[] { "--vim=true", ".name" });
		assertThat(explicitVim.getOptionValue("vim")).isEqualTo("true");
		assertThat(explicitVim.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(explicitVim)).isTrue();

		CommandLine autoVim = Main.createCommandLineParser().parse(options, new String[] { "--vim=auto", ".name" });
		assertThat(autoVim.getOptionValue("vim")).isEqualTo("auto");
		assertThat(autoVim.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(autoVim)).isFalse();

		CommandLine disabledVim = Main.createCommandLineParser().parse(options, new String[] { "--vim=false", ".name" });
		assertThat(disabledVim.getOptionValue("vim")).isEqualTo("false");
		assertThat(disabledVim.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(disabledVim)).isFalse();

		CommandLine interactiveAuto = Main.createCommandLineParser()
				.parse(options, new String[] { "--interactive", "--vim=auto", ".name" });
		assertThat(interactiveAuto.getOptionValue("vim")).isEqualTo("auto");
		assertThat(interactiveAuto.getArgList()).containsExactly(".name");
		assertThat(Main.isInteractive(interactiveAuto)).isTrue();

		CommandLine escaped = Main.createCommandLineParser().parse(options, new String[] { "--", "--vim" });
		assertThat(escaped.getOptionValue("vim")).isNull();
		assertThat(escaped.getArgList()).containsExactly("--vim");
		assertThat(Main.isInteractive(escaped)).isFalse();
	}

	@Test
	void resolvesVimModeFromEditorAndExplicitOverrides() throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder().longOpt("vim").hasArg().get());

		CommandLine automatic = Main.createCommandLineParser().parse(options, new String[] { "--interactive" });
		assertThat(Main.isVimMode(automatic, "vi")).isTrue();
		assertThat(Main.isVimMode(automatic, "vim")).isTrue();
		assertThat(Main.isVimMode(automatic, "vim -f")).isTrue();
		assertThat(Main.isVimMode(automatic, "/usr/bin/vim")).isTrue();
		assertThat(Main.isVimMode(automatic, "/usr/bin/vi")).isTrue();
		assertThat(Main.isVimMode(automatic, "'/path with spaces/vim' -f")).isTrue();
		assertThat(Main.isVimMode(automatic, "\"/path with spaces/vi\" --clean")).isTrue();
		assertThat(Main.isVimMode(automatic, null)).isFalse();
		assertThat(Main.isVimMode(automatic, "")).isFalse();
		assertThat(Main.isVimMode(automatic, "   ")).isFalse();
		assertThat(Main.isVimMode(automatic, "nano")).isFalse();
		assertThat(Main.isVimMode(automatic, "nvim")).isFalse();
		assertThat(Main.isVimMode(automatic, "vim90")).isFalse();
		assertThat(Main.isVimMode(automatic, "gvim")).isFalse();

		CommandLine explicitAuto = Main.createCommandLineParser()
				.parse(options, new String[] { "--interactive", "--vim=auto" });
		assertThat(Main.isVimMode(explicitAuto, "vim")).isTrue();
		assertThat(Main.isVimMode(explicitAuto, "nano")).isFalse();

		CommandLine bare = Main.createCommandLineParser().parse(options, new String[] { "--vim" });
		assertThat(Main.isVimMode(bare, "nano")).isTrue();
		assertThat(Main.isVimMode(bare, null)).isTrue();

		CommandLine enabled = Main.createCommandLineParser().parse(options, new String[] { "--vim=true" });
		assertThat(Main.isVimMode(enabled, "nano")).isTrue();
		assertThat(Main.isVimMode(enabled, null)).isTrue();

		CommandLine disabled = Main.createCommandLineParser()
				.parse(options, new String[] { "--interactive", "--vim=false" });
		assertThat(Main.isVimMode(disabled, "vim")).isFalse();
		assertThat(Main.isVimMode(disabled, "/usr/bin/vi")).isFalse();
	}

	@Test
	void rejectsUnknownVimMode() throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder().longOpt("vim").hasArg().get());

		CommandLine command = Main.createCommandLineParser().parse(options, new String[] { "--vim=maybe" });
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.isInteractive(command))
				.withMessage("invalid --vim: maybe (expected one of: auto, true, false)");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.isVimMode(command, "vim"))
				.withMessage("invalid --vim: maybe (expected one of: auto, true, false)");

		CommandLine interactiveCommand = Main.createCommandLineParser()
				.parse(options, new String[] { "--interactive", "--vim=invalid" });
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Main.isInteractive(interactiveCommand))
				.withMessage("invalid --vim: invalid (expected one of: auto, true, false)");
	}

	@Test
	void parsesInteractiveNullInputAsClusteredShortOptions() throws ParseException {
		Options options = interactiveNullInputOptions();

		CommandLine clustered = Main.createCommandLineParser().parse(options, new String[] { "-in" });
		CommandLine separate = Main.createCommandLineParser().parse(options, new String[] { "-i", "-n" });
		CommandLine longOptions = Main.createCommandLineParser().parse(options, new String[] { "--interactive", "--null-input" });

		for (CommandLine command : new CommandLine[] { clustered, separate, longOptions }) {
			assertThat(command.hasOption("interactive")).isTrue();
			assertThat(command.hasOption("null-input")).isTrue();
			assertThat(command.getArgList()).isEmpty();
		}
	}

	@Test
	void rejectsPartialLongOptionNames() {
		assertThatThrownBy(() -> Main.createCommandLineParser()
				.parse(interactiveNullInputOptions(), new String[] { "--inter" }))
				.isInstanceOf(UnrecognizedOptionException.class);
	}

	@Test
	void interactiveNullInputDoesNotReadImplicitOrExplicitStdin() throws Exception {
		assertInteractiveNullInputDoesNotReadStdin(Collections.emptyList());
		assertInteractiveNullInputDoesNotReadStdin(Collections.singletonList("-"));
	}

	@Test
	void testCreateDefaultRunnerWhenDevTtyAvailable() {
		if (new File("/dev/tty").exists()) {
			try (TuiRunner runner = Main.createDefaultRunner()) {
				assertThat(runner).isNotNull();
			} catch (Exception ignored) {
				// /dev/tty may exist but not be openable in sandbox/headless environments
			}
		}
	}

	private static TuiRunner createTestRunner(String keyInput, ByteArrayOutputStream terminalOut) throws Exception {
		LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", terminalOut, StandardCharsets.UTF_8);
		terminal.setSize(new Size(80, 24));
		terminal.processInputBytes(keyInput.getBytes(StandardCharsets.UTF_8));
		return TuiRunner.create(TuiConfig.builder()
				.rawMode(false)
				.shutdownHook(false)
				.alternateScreen(false)
				.hideCursor(false)
				.backend(new JLineBackend(terminal))
				.build());
	}

	private static Options interactiveNullInputOptions() {
		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("interactive").get());
		options.addOption(Option.builder("n").longOpt("null-input").get());
		return options;
	}

	private static void assertInteractiveNullInputDoesNotReadStdin(List<String> inputFiles) throws Exception {
		CommandLineParser parser = Main.createCommandLineParser();
		CommandLine command = parser.parse(interactiveNullInputOptions(), new String[] { "-in" });
		TuiRunner runner = createTestRunner("", new ByteArrayOutputStream());
		runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
		runner.dispatch(KeyEvent.ofChar('y'));

		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try {
			System.setIn(new InputStream() {
				@Override
				public int read() {
					throw new AssertionError("stdin must not be read with --null-input");
				}
			});
			System.setOut(new PrintStream(out));
			System.setErr(new PrintStream(err));
			Main.run(command, ".", inputFiles, Versions.JQ_1_6,
					Jackson3JsonProvider.getInstance(),
					RuntimeOptions.newBuilder().build(), warnTypeChecking(), runner, null);
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}

		assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("null\n");
		String expectedCommand = inputFiles.isEmpty()
				? "jackson-jq -n -- '.'\n"
				: "jackson-jq -n -- '.' '-'\n";
		assertThat(err.toString(StandardCharsets.UTF_8)).isEqualTo(expectedCommand);
	}

	private static Path write(Path dir, String name, String content) throws Exception {
		Path file = dir.resolve(name);
		Files.writeString(file, content);
		return file;
	}

	// What the CLI compiles with by default: everything else left alone, type checking on and warning.
	private static CompileOptions warnTypeChecking() {
		return CompileOptions.newBuilder().setTypeCheckMode(TypeCheckMode.WARN).build();
	}

	private static CommandLine parseLimits(String... args) throws Exception {
		Options options = new Options();
		options.addOption(Option.builder().longOpt("max-string-length").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("max-binary-length").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("max-array-length").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("max-object-member-count").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("max-user-defined-function-calls").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("max-outputs-per-expression").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("type-check").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("input-type").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("output-type").numberOfArgs(1).get());
		options.addOption(Option.builder().longOpt("disable-tco").get());
		return new DefaultParser().parse(options, args);
	}

	private static String run(String input, String... args) throws Exception {
		return capture(input, args).out();
	}

	private static String runStderr(String input, String... args) throws Exception {
		return capture(input, args).err();
	}

	private record Captured(String out, String err) {
	}

	private static synchronized Captured capture(String input, String... args) throws Exception {
		InputStream originalIn = System.in;
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		try {
			System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			System.setOut(new PrintStream(output));
			System.setErr(new PrintStream(errors));
			Main.main(args);
			return new Captured(output.toString(StandardCharsets.UTF_8),
					errors.toString(StandardCharsets.UTF_8));
		} finally {
			System.setIn(originalIn);
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
	}
}
