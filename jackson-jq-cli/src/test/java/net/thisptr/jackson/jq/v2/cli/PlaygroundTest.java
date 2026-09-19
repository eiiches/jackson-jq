package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.tamboui.backend.jline3.JLineBackend;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.TickEvent;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.OptimizationOptions;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

class PlaygroundTest {

	private static final Jackson3JsonProvider JSON = Jackson3JsonProvider.getInstance();

	@Test
	void initializesWithEvaluatedPreview() {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				System.out,
				System.err);

		assertThat(pg.getQuery()).isEqualTo(".");
		assertThat(pg.getErrorMessage()).isNull();
		assertThat(pg.getInputLines()).isNotEmpty();
		assertThat(String.join("\n", pg.getInputLines())).contains("\"name\": \"Alice\"");
		assertThat(pg.getPreviewLines()).isNotEmpty();
		assertThat(String.join("\n", pg.getPreviewLines())).contains("\"name\": \"Alice\"");
	}

	@Test
	void formatsFinalCommandWithEffectiveOptionsAndShellQuoting() {
		RuntimeOptions runtimeOptions = RuntimeOptions.newBuilder()
				.setMaxStringLength(1)
				.setMaxBinaryLength(2)
				.setMaxArrayLength(3)
				.setMaxObjectMemberCount(4)
				.setMaxUserDefinedFunctionCalls(5)
				.setMaxOutputsPerExpression(6)
				.build();
		CompileOptions compileOptions = CompileOptions.newBuilder()
				.setOptimizationOptions(OptimizationOptions.newBuilder()
						.setTailCallOptimization(false)
						.build())
				.build();
		Playground<JsonNode> pg = new Playground<>(
				Main.createEnvironment(JSON, Versions.JQ_1_7),
				Versions.JQ_1_7,
				"gson",
				null,
				true,
				true,
				true,
				".[\"it's\"]\n| .",
				JSON,
				runtimeOptions,
				compileOptions,
				true,
				true,
				false,
				Arrays.asList("input data.json", "odd'name.json"),
				System.out,
				System.err);

		assertThat(pg.finalCommand()).isEqualTo(
				"jackson-jq -c -r -n -R -s --jq 1.7.0 --json-provider gson --no-warnings --disable-tco"
						+ " --max-string-length 1 --max-binary-length 2 --max-array-length 3"
						+ " --max-object-member-count 4 --max-user-defined-function-calls 5"
						+ " --max-outputs-per-expression 6 -- '.[\"it'\"'\"'s\"]\n| .'"
						+ " 'input data.json' 'odd'\"'\"'name.json'");
	}

	@Test
	void pausesAutomaticEvaluationAndKeepsLastPreview() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('p', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		events.addAll(textToKeys(".name"));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(createTestRunner(terminalOut, events));

		assertThat(pg.getQuery()).isEqualTo(".name");
		assertThat(pg.getErrorMessage()).isNull();
		assertThat(pg.isAutomaticEvaluationPaused()).isTrue();
		assertThat(pg.isOutputStale()).isTrue();
		assertThat(String.join("\n", pg.getPreviewLines())).contains("\"name\": \"Alice\"");
		assertThat(lineToPlainText(pg.buildAutoRunStatusLine()))
				.isEqualTo(" Auto-run: Paused (Ctrl+P to toggle) ");
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Auto-run: On", "Auto-run: Paused", "(Ctrl+P to toggle)", "(Stal");
	}

	@Test
	void manualEvaluationWorksFromOutputWhilePaused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('p', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		events.addAll(textToKeys(".name"));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('r', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(createTestRunner(terminalOut, events));

		assertThat(pg.getFocus()).isEqualTo(Playground.Focus.OUTPUT);
		assertThat(pg.isAutomaticEvaluationPaused()).isTrue();
		assertThat(pg.isOutputStale()).isFalse();
		assertThat(pg.getPreviewLines()).containsExactly("\"Alice\"");
	}

	@Test
	void resumingImmediatelyEvaluatesPendingChanges() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('p', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		events.addAll(textToKeys(".name"));
		events.add(KeyEvent.ofChar('p', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(createTestRunner(terminalOut, events));

		assertThat(pg.isAutomaticEvaluationPaused()).isFalse();
		assertThat(pg.isOutputStale()).isFalse();
		assertThat(pg.getPreviewLines()).containsExactly("\"Alice\"");
		assertThat(lineToPlainText(pg.buildAutoRunStatusLine()))
				.isEqualTo(" Auto-run: On (Ctrl+P to toggle) ");
		assertThat(new String(terminalOut.toByteArray(), StandardCharsets.UTF_8))
				.contains("Auto-run: On", "(Ctrl+P to toggle)");
	}

	@Test
	void pausedEditsStillCompileAndModalShortcutsAreInert() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('p', KeyModifiers.CTRL),
				KeyEvent.ofChar('u', KeyModifiers.CTRL),
				KeyEvent.ofChar('['),
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('p', KeyModifiers.CTRL),
				KeyEvent.ofChar('r', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(runner);

		assertThat(pg.isAutomaticEvaluationPaused()).isTrue();
		assertThat(pg.isRawOutput()).isFalse();
		assertThat(pg.getProviderName()).isEqualTo("jackson3");
		assertThat(pg.getErrorMessage()).isNotNull();
		assertThat(pg.isOutputStale()).isTrue();
		assertThat(String.join("\n", pg.getPreviewLines())).contains("\"name\": \"Alice\"");
	}

	@Test
	void handlesSyntaxErrorGracefully() {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				". | ",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				System.out,
				System.err);

		assertThat(pg.getErrorMessage()).isNotNull();
	}

	@Test
	void escapeEmitsResultsOnConfirm() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(10)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("10\n");
		assertThat(new String(err.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("jackson-jq -c -- '.a'\n");
	}

	@Test
	void ctrlCCancelsWithoutEmittingResults() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(10)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('c', KeyModifiers.CTRL),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEmpty();
		assertThat(new String(err.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("jackson-jq -c -- '.a'\n");
	}

	@Test
	void dismissesConfirmationModalOnCancel() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(10)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Cancel the quit modal with 'n', cancel the submit modal with Ctrl+C, then submit
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('c', KeyModifiers.CTRL),
				KeyEvent.ofChar('n'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('c', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("10\n");
	}

	@Test
	void ctrlCQuitsFromSearchAndOptions() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofChar('/'),
				KeyEvent.ofChar('f'),
				KeyEvent.ofChar('c', KeyModifiers.CTRL),
				KeyEvent.ofChar('n'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('c', KeyModifiers.CTRL),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createObject(Collections.singletonMap("foo", JSON.createNumber(1)))),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(out.toByteArray()).isEmpty();
		assertThat(new String(terminalOut.toByteArray(), StandardCharsets.UTF_8)).contains("Quit Playground");
	}

	@Test
	void handlesMultilineQueryWithNewlines() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("val", JSON.createNumber(99)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Start with query ".", type "val", Enter (newline), type "+ 1", then Escape + 'y'
		List<Event> events = new ArrayList<>(textToKeys("val\n+ 1"));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getQuery()).isEqualTo(".val\n+ 1");
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("100\n");
		assertThat(new String(err.toByteArray(), StandardCharsets.UTF_8))
				.isEqualTo("jackson-jq -c -- '.val\n+ 1'\n");
	}

	@Test
	void switchesFocusWithTabAndScrolls() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Press Tab 3 times to switch focus to Output, Esc to open submit modal, then confirm
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("1\n");
	}

	@Test
	void cyclesFocusThroughQueryInputOutput() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Tab -> DIAGNOSTICS, Tab -> INPUT, Tab -> OUTPUT, Tab -> QUERY, Shift+Tab -> OUTPUT, Escape -> submit modal, y -> confirm
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(new String(out.toByteArray(), StandardCharsets.UTF_8)).isEqualTo("1\n");
	}

	@Test
	void ignoresPrintableCharactersWhenInputPaneIsFocused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofChar('x'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getFocus()).isEqualTo(Playground.Focus.INPUT);
		assertThat(pg.getQuery()).isEqualTo(".a");
	}

	@Test
	void ignoresPrintableCharactersWhenOutputPaneIsFocused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofChar('x'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".a",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getFocus()).isEqualTo(Playground.Focus.OUTPUT);
		assertThat(pg.getQuery()).isEqualTo(".a");
	}

	@Test
	void scrollsInputPaneWhenFocused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> map = new LinkedHashMap<>();
		for (int i = 0; i < 30; i++) {
			map.put("key" + i, JSON.createNumber(i));
		}
		JsonNode input = JSON.createObject(map);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Tab twice to focus INPUT, Down 3 times to select node, then Escape + 'y' to submit
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".key0",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getInputSelectedIndex()).isEqualTo(3);
	}

	@Test
	void clampsScrollingToContentBounds() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> map = new LinkedHashMap<>();
		for (int i = 0; i < 30; i++) {
			map.put("key" + i, JSON.createNumber(i));
		}
		JsonNode input = JSON.createObject(map);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Tab twice to focus INPUT, press End (jump to bottom), Home (jump to top), Down 100 times, then Escape + 'y'
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.END));
		events.add(KeyEvent.ofKey(KeyCode.HOME));
		for (int i = 0; i < 100; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".key0",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		int maxIndex = pg.getInputTreePane().lastFlatEntries().size() - 1;
		assertThat(maxIndex).isGreaterThan(0);
		assertThat(pg.getInputSelectedIndex()).isEqualTo(maxIndex);
	}

	@Test
	void scrollsOutputPaneWhenFocused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createNull();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Tab 3 times to focus OUTPUT, press End (jump to bottom), Home (jump to top), Down 5 times, then Escape + 'y'
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.END));
		events.add(KeyEvent.ofKey(KeyCode.HOME));
		for (int i = 0; i < 5; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				"[range(50)]",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getOutputSelectedIndex()).isEqualTo(5);
	}

	@Test
	void scrollsPanesByHalfPageWithCtrlDAndCtrlU() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> map = new LinkedHashMap<>();
		for (int i = 0; i < 30; i++) {
			map.put("key" + i, JSON.createNumber(i));
		}

		TuiRunner inputRunner = createTestRunner(
				new ByteArrayOutputStream(),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofChar('d', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));
		Playground<JsonNode> inputPlayground = new Playground<>(
				env,
				Collections.singletonList(JSON.createObject(map)),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		inputPlayground.run(inputRunner);

		JsonTreePane inputPane = inputPlayground.getInputTreePane();
		assertThat(inputPlayground.getInputSelectedIndex()).isEqualTo(Math.max(1, inputPane.viewportHeight() / 2));

		TuiRunner outputRunner = createTestRunner(
				new ByteArrayOutputStream(),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.END),
				KeyEvent.ofChar('u', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));
		Playground<JsonNode> outputPlayground = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				"[range(50)]",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		outputPlayground.run(outputRunner);

		JsonTreePane outputPane = outputPlayground.getOutputTreePane();
		int maxScroll = outputPane.textLines().size() - outputPane.viewportHeight();
		assertThat(outputPlayground.getOutputSelectedIndex())
				.isEqualTo(maxScroll - Math.max(1, outputPane.viewportHeight() / 2));
	}

	@Test
	void retainsAndScrollsToEndOfLargeInputsAndOutputs() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		List<JsonNode> inputs = new ArrayList<>();
		for (int i = 0; i < 5001; i++) {
			inputs.add(JSON.createNumber(i));
		}

		TuiRunner runner = createTestRunner(
				new ByteArrayOutputStream(),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.END),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.END),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));
		Playground<JsonNode> pg = new Playground<>(
				env,
				inputs,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));
		JsonTreePane inputPane = pg.getInputTreePane();
		inputPane.toggleViewMode();

		pg.run(runner);

		JsonTreePane outputPane = pg.getOutputTreePane();
		assertThat(pg.getInputLines()).hasSize(5001).endsWith("5000");
		assertThat(pg.getPreviewLines()).hasSize(5001).endsWith("5000");
		assertThat(inputPane.roots()).hasSize(5001);
		assertThat(outputPane.roots()).hasSize(5001);
		assertThat(inputPane.textScrollOffset())
				.isEqualTo(inputPane.textLines().size() - inputPane.viewportHeight());
		assertThat(outputPane.textScrollOffset())
				.isEqualTo(outputPane.textLines().size() - outputPane.viewportHeight());
	}

	@Test
	void rendersNavigationGuideAcrossFocusStates() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createNumber(42);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		// Tab to DIAGNOSTICS, Tab to INPUT, Tab to OUTPUT, Tab to QUERY, then Escape + 'y'
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("╭");
		assertThat(rendered).contains("╰");
	}

	@Test
	void collectsAndDisplaysWarningsForAmbiguousQuery() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createNull();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				"1, 2 | .",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				true,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getWarnings()).isNotEmpty();
		assertThat(pg.getWarnings().get(0).message()).contains("binds tighter than");

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("[Warning]");
		assertThat(rendered).contains("Output Preview [Text]: 2 items");
		assertThat(rendered).contains("Diagnostics");

		// Warnings should NOT be printed to stderr upon acceptance
		String stderrText = new String(err.toByteArray(), StandardCharsets.UTF_8);
		assertThat(stderrText).doesNotContain("jq: warning:");
		assertThat(stderrText).isEqualTo("jackson-jq -c -- '1, 2 | .'\n");
	}

	@Test
	void suppressesWarningsWhenDisabled() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createNull();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				"1, 2 | .",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				false,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getWarnings()).isEmpty();

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).doesNotContain("[Warning]");
		assertThat(rendered).contains("Output Preview [Text]: 2 items");
		assertThat(rendered).contains("(no diagnostics)");
	}

	@Test
	void displaysMultipleWarningsWithDynamicExpansion() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createNull();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				"1, 2 | ., 3 | .",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				true,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getWarnings().size()).isGreaterThanOrEqualTo(2);

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("[Warning]");
	}

	@Test
	void displaysStatsInPaneTitlesAndKeepsGreenResultsOnFocus() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				100,
				24,
				KeyEvent.ofKey(KeyCode.TAB), // Focus -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.TAB), // Focus -> INPUT
				KeyEvent.ofKey(KeyCode.TAB), // Focus -> OUTPUT
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Input [Tree]: 1 item (3 lines)");
		assertThat(rendered).contains("Output Preview [Text]: 1 item (3 lines)");
	}

	@Test
	void retainsPreviousOutputAndTitleOnError() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("a", JSON.createNumber(1)));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				100,
				24,
				KeyEvent.ofChar('['),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(out),
				new PrintStream(err));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getOutputTreePane().roots()).isNotEmpty();
		assertThat(pg.getOutputTreePane().textLines()).isNotEmpty();
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("[Error]");
		assertThat(rendered).contains("Output Preview [Text]: 1 item (3 lines)");
		assertThat(rendered).contains("\"a\":");
	}

	@Test
	void opensAndClosesOptionsDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('q'),
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
	}

	@Test
	void navigatesAndTogglesOptionsInDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofChar(' '),
				KeyEvent.ofKey(KeyCode.UP),
				KeyEvent.ofChar(' '),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"hello\nworld\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isRawInput()).isTrue();
		assertThat(pg.isSlurp()).isTrue();
		assertThat(pg.isCompact()).isFalse();
		assertThat(pg.isRawOutput()).isFalse();
	}

	@Test
	void togglesOptionsViaDirectShortcutsInDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('r'),
				KeyEvent.ofChar('c'),
				KeyEvent.ofChar('s'),
				KeyEvent.ofChar('R'),
				KeyEvent.ofKey(KeyCode.ENTER),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"\"abc\"\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isRawInput()).isTrue();
		assertThat(pg.isSlurp()).isTrue();
		assertThat(pg.isCompact()).isTrue();
		assertThat(pg.isRawOutput()).isTrue();
	}

	@Test
	void togglingRawInputAndSlurpUpdatesInputsAndEvaluationLive() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('s'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n{\"a\": 2}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		assertThat(pg.getInputLines()).containsExactly("{", "  \"a\": 1", "}", "{", "  \"a\": 2", "}");

		pg.run(runner);

		assertThat(pg.isSlurp()).isTrue();
		String inputJoined = String.join("\n", pg.getInputLines());
		assertThat(inputJoined).startsWith("[");
		assertThat(inputJoined).endsWith("]");
	}

	@Test
	void compactOptionAffectsOutputPreviewOnlyAndNotInputs() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('c'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		assertThat(pg.getInputLines()).containsExactly("{", "  \"a\": 1", "}");
		assertThat(pg.getPreviewLines()).containsExactly("{", "  \"a\": 1", "}");

		pg.run(runner);

		assertThat(pg.isCompact()).isTrue();
		assertThat(pg.getPreviewLines()).containsExactly("{\"a\":1}");
		assertThat(pg.getInputLines()).containsExactly("{", "  \"a\": 1", "}");
	}

	@Test
	void cyclesJqVersionInOptionsDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('v'),
				KeyEvent.ofChar('v'),
				KeyEvent.ofChar('V'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		assertThat(pg.getVersion()).isEqualTo(Versions.JQ_1_6);

		pg.run(runner);

		assertThat(pg.getVersion()).isEqualTo(Versions.JQ_1_7);
	}

	@Test
	void cyclesJsonProviderInOptionsDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofChar('p'),
				KeyEvent.ofChar('p'),
				KeyEvent.ofChar('P'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		assertThat(pg.getProviderName()).isEqualTo("jackson3");

		pg.run(runner);

		assertThat(pg.getProviderName()).isEqualTo("jackson2");
		assertThat(pg.getInputLines()).containsExactly("{", "  \"a\": 1", "}");
		assertThat(pg.getPreviewLines()).containsExactly("{", "  \"a\": 1", "}");
	}

	@Test
	void navigatesAndCyclesVersionAndProviderWithArrowKeys() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofKey(KeyCode.RIGHT),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofChar(' '),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getVersion()).isEqualTo(Versions.JQ_1_7);
		assertThat(pg.getProviderName()).isEqualTo("jackson2");
	}

	@Test
	void editsRuntimeLimitsInOptionsDialog() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		for (int i = 0; i < 6; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofChar('1'));
		events.add(KeyEvent.ofChar('2'));
		events.add(KeyEvent.ofKey(KeyCode.BACKSPACE));
		events.add(KeyEvent.ofKey(KeyCode.BACKSPACE));
		events.add(KeyEvent.ofKey(KeyCode.BACKSPACE));
		events.add(KeyEvent.ofChar('3'));
		for (char value = '4'; value <= '8'; value++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
			events.add(KeyEvent.ofChar(value));
		}
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(createTestRunner(terminalOut, events));

		RuntimeOptions options = pg.getRuntimeOptions();
		assertThat(options.getMaxStringLength()).isEqualTo(3);
		assertThat(options.getMaxBinaryLength()).isEqualTo(4);
		assertThat(options.getMaxArrayLength()).isEqualTo(5);
		assertThat(options.getMaxObjectMemberCount()).isEqualTo(6);
		assertThat(options.getMaxUserDefinedFunctionCalls()).isEqualTo(7);
		assertThat(options.getMaxOutputsPerExpression()).isEqualTo(8);
		assertThat(pg.getSelectedOptionIndex()).isEqualTo(11);
	}

	@Test
	void ignoresRuntimeLimitDigitsThatWouldOverflow() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		RuntimeOptions initialOptions = RuntimeOptions.newBuilder()
				.setMaxStringLength(Integer.MAX_VALUE - 1)
				.setMaxUserDefinedFunctionCalls(Long.MAX_VALUE - 1)
				.build();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		for (int i = 0; i < 6; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofChar('9'));
		for (int i = 0; i < 4; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofChar('9'));
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				initialOptions,
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(createTestRunner(new ByteArrayOutputStream(), events));

		assertThat(pg.getRuntimeOptions().getMaxStringLength()).isEqualTo(Integer.MAX_VALUE - 1);
		assertThat(pg.getRuntimeOptions().getMaxUserDefinedFunctionCalls()).isEqualTo(Long.MAX_VALUE - 1);
	}

	@Test
	void runtimeLimitEditsUpdatePreviewAndAcceptedEvaluation() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		for (int i = 0; i < 6; i++) {
			events.add(KeyEvent.ofKey(KeyCode.DOWN));
		}
		events.add(KeyEvent.ofChar('3'));
		events.add(KeyEvent.ofChar('o', KeyModifiers.CTRL));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createString("abc")),
				". + \"d\"",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(err));

		pg.run(createTestRunner(new ByteArrayOutputStream(), events));

		assertThat(pg.getErrorMessage()).contains("maximum string length of 3");
		assertThat(new String(err.toByteArray(), StandardCharsets.UTF_8))
				.contains("maximum string length of 3");
	}

	@Test
	void rendersOptionDialogOverlay() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		RuntimeOptions runtimeOptions = RuntimeOptions.newBuilder()
				.setMaxStringLength(10101)
				.setMaxBinaryLength(20202)
				.setMaxArrayLength(30303)
				.setMaxObjectMemberCount(40404)
				.setMaxUserDefinedFunctionCalls(50505)
				.build();
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofChar('o', KeyModifiers.CTRL),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				"{\"a\": 1}\n".getBytes(StandardCharsets.UTF_8),
				false,
				false,
				false,
				".",
				JSON,
				runtimeOptions,
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Options");
		assertThat(rendered).contains("Input:");
		assertThat(rendered).contains("-R, --raw-input");
		assertThat(rendered).contains("-s, --slurp");
		assertThat(rendered).contains("Output:");
		assertThat(rendered).contains("-c, --compact");
		assertThat(rendered).contains("-r, --raw-output");
		assertThat(rendered).contains("Engine:");
		assertThat(rendered).contains("Version:");
		assertThat(rendered).contains("< " + Versions.JQ_1_6 + " >");
		assertThat(rendered).contains("Provider:");
		assertThat(rendered).contains("< jackson3 >");
		assertThat(rendered).contains("Runtime Limits:");
		assertThat(rendered).contains("--max-string-length");
		assertThat(rendered).contains("--max-binary-length");
		assertThat(rendered).contains("--max-array-length");
		assertThat(rendered).contains("--max-object-member-count");
		assertThat(rendered).contains("--max-user-defined-function-calls");
		assertThat(rendered).contains("--max-outputs-per-expression");
		assertThat(rendered).contains("10101", "20202", "30303", "40404", "50505");
		assertThat(rendered).contains("unlimited");
		assertThat(rendered).contains("[0-9/⌫] Edit");
		assertThat(rendered).contains("[Space/←→] Change");
		assertThat(rendered).contains("[Esc/Enter] Close");
		assertThat(rendered).contains("Ctrl+O");
	}

	@Test
	void rendersDiagnosticsPaneWhenNoDiagnostics() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getDiagnosticPlainLines()).containsExactly("(no diagnostics)");
		assertThat(pg.getDiagnosticsViewportHeight()).isEqualTo(1);
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Diagnostics");
		assertThat(rendered).contains("(no diagnostics)");
	}

	@Test
	void rendersMultilineCompileErrorInDiagnosticsPane() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				"[",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		List<String> diagLines = pg.getDiagnosticPlainLines();
		assertThat(diagLines.size()).isGreaterThanOrEqualTo(3);
		assertThat(diagLines.get(0)).startsWith("[Error] syntax error");
		assertThat(diagLines.get(1)).isEqualTo("    [");
		assertThat(diagLines.get(2)).isEqualTo("    ^");
		assertThat(pg.getDiagnosticsViewportHeight()).isEqualTo(3);

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("[Error]");
		assertThat(rendered).contains("Diagnostics");
	}

	@Test
	void rendersMultipleWarningsInDiagnosticsPane() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				"1, 2 | ., 3 | .",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				true,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		List<String> diagLines = pg.getDiagnosticPlainLines();
		assertThat(diagLines.size()).isGreaterThanOrEqualTo(2);
		for (String line : diagLines) {
			assertThat(line).startsWith("[Warning]");
		}
		assertThat(pg.getDiagnosticsViewportHeight()).isEqualTo(diagLines.size());

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("[Warning]");
		assertThat(rendered).contains("Diagnostics");
	}

	@Test
	void capsDiagnosticsPaneViewportAtFiveContentRowsAndScrolls() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		String query = "1, 2 | ., 3 | ., 4 | ., 5 | ., 6 | ., 7 | .";
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB)); // Focus -> DIAGNOSTICS
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // Scroll down 1
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // Scroll down 2
		events.add(KeyEvent.ofKey(KeyCode.END));  // Scroll to bottom
		events.add(KeyEvent.ofKey(KeyCode.HOME)); // Scroll to top
		events.add(KeyEvent.ofKey(KeyCode.PAGE_DOWN)); // Page down
		events.add(KeyEvent.ofKey(KeyCode.PAGE_UP));   // Page up
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // Scroll down 1
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				query,
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getDiagnosticPlainLines().size()).isGreaterThan(5);
		assertThat(pg.getDiagnosticsViewportHeight()).isEqualTo(5);
		assertThat(pg.getDiagnosticsScrollOffset()).isEqualTo(1);

		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Diagnostics");
	}

	@Test
	void cyclesFocusThroughDiagnostics() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB), // -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.TAB), // -> INPUT
				KeyEvent.ofKey(KeyCode.TAB), // -> OUTPUT
				KeyEvent.ofKey(KeyCode.TAB), // -> QUERY
				KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT), // -> OUTPUT
				KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT), // -> INPUT
				KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT), // -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT), // -> QUERY
				KeyEvent.ofKey(KeyCode.TAB), // -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.ESCAPE), // Open submit modal
				KeyEvent.ofChar('y')); // Confirm

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
	}

	@Test
	void rendersDiagnosticsFocusedStyleAndGuide() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		TuiRunner runner = createTestRunner(
				terminalOut,
				120,
				24,
				KeyEvent.ofKey(KeyCode.TAB), // Focus -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getFocus()).isEqualTo(Playground.Focus.DIAGNOSTICS);
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("Scroll");
		assertThat(rendered).contains("Focus Next");
		assertThat(rendered).contains("Emit & Quit");
		assertThat(rendered).contains("Quit");
		assertThat(rendered).doesNotContain("Accept");
	}

	@Test
	void resetsDiagnosticsScrollOffsetOnRecompilation() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		String query = "1, 2 | ., 3 | ., 4 | ., 5 | ., 6 | ., 7 | .";
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB)); // Focus -> DIAGNOSTICS
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // Scroll offset = 1
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // Scroll offset = 2
		events.add(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT)); // Focus -> QUERY
		events.add(KeyEvent.ofChar(' ')); // Edit query -> triggers updateEvaluation
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				query,
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getDiagnosticsScrollOffset()).isEqualTo(0);
	}

	@Test
	void rendersAtMinimumSupportedTerminalHeight() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);

		// Height 10: supported minimum height (Query 4, Diagnostics 3, Input/Output 3)
		ByteArrayOutputStream out10 = new ByteArrayOutputStream();
		TuiRunner runner10 = createTestRunner(
				out10, 80, 10,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg10 = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg10.run(runner10);

		String rendered10 = new String(out10.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered10).contains("Query");
		assertThat(rendered10).contains("Diagnostics");
		assertThat(rendered10).contains("Input");
		assertThat(rendered10).contains("Output Preview");
		assertThat(rendered10).doesNotContain("too small");

		// Height 9: below supported minimum
		ByteArrayOutputStream out9 = new ByteArrayOutputStream();
		TuiRunner runner9 = createTestRunner(
				out9, 80, 9,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg9 = new Playground<>(
				env,
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				true,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg9.run(runner9);

		String rendered9 = new String(out9.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered9).contains("Screen", "small", "playground");
	}

	@Test
	void initializesWithUpToDateStatusIndicator() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(runner);

		assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.UP_TO_DATE);
		assertThat(lineToPlainText(pg.buildAutoRunStatusLine()))
				.isEqualTo(" Auto-run: On (Ctrl+P to toggle) ");
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("✓ Up to date");
		assertThat(rendered).contains("Auto-run: On", "(Ctrl+P to toggle)");
	}

	@Test
	void showsPendingRunStatusIndicatorWhenEditedWhilePaused() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('p', KeyModifiers.CTRL));
		events.addAll(textToKeys("name"));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		TuiRunner runner = createTestRunner(terminalOut, events);
		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(runner);

		assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.STALE);
		assertThat(lineToPlainText(pg.buildAutoRunStatusLine()))
				.isEqualTo(" Auto-run: Paused (Ctrl+P to toggle) ");
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("○ Pending Run");
		assertThat(rendered).doesNotContain("⚠ Stale");
		assertThat(rendered).contains("Auto-run: Paused", "(Ctrl+P to toggle)");
	}

	@Test
	void showsErrorStatusIndicatorOnSyntaxError() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ". | [", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(runner);

		assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.FAILURE);
		String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(rendered).contains("✗ Error");
	}

	@Test
	void showsLoadingSpinnerWhileAsyncEvaluatingAndAdvancesOnTick() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		CountDownLatch evalStarted = new CountDownLatch(1);
		CountDownLatch allowEvalToFinish = new CountDownLatch(1);
		CountDownLatch evalDone = new CountDownLatch(1);

		ExecutorService controlledExecutor = Executors.newSingleThreadExecutor();
		try {
			TuiRunner runner = createTestRunner(
					terminalOut,
					KeyEvent.ofChar('a'),
					TickEvent.of(1, Duration.ofMillis(250)));

			Playground<JsonNode> pg = new Playground<>(
					env, Collections.singletonList(input), ".", JSON,
					RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
					false, false, System.out, System.err);

			pg.setEvaluationExecutor(command -> {
				controlledExecutor.execute(() -> {
					evalStarted.countDown();
					try {
						allowEvalToFinish.await(5, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					try {
						command.run();
					} finally {
						evalDone.countDown();
					}
				});
			});

			Thread runThread = new Thread(() -> {
				try {
					pg.run(runner);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			runThread.start();

			assertThat(evalStarted.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.LOADING);

			allowEvalToFinish.countDown();
			assertThat(evalDone.await(5, TimeUnit.SECONDS)).isTrue();

			// Give the runner events to process render callback, then confirm and quit
			runner.dispatch(TickEvent.of(2, Duration.ofMillis(250)));
			runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
			runner.dispatch(KeyEvent.ofChar('y'));
			runThread.join(5000);

			assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.UP_TO_DATE);
			String rendered = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
			assertThat(rendered).contains("Evaluating...");
			assertThat(rendered).contains("✓ Up to date");
		} finally {
			controlledExecutor.shutdownNow();
		}
	}

	@Test
	void tickEventDoesNotAdvanceWhenNotLoading() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				TickEvent.of(1, Duration.ofMillis(250)),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env, Collections.singletonList(input), ".", JSON,
				RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
				false, false, System.out, System.err);

		pg.run(runner);

		assertThat(pg.getSpinnerState().tick()).isEqualTo(0);
	}

	@Test
	void supersedesPreviousAsyncEvaluationsOnRapidTyping() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		CountDownLatch finalTaskDone = new CountDownLatch(1);
		ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
		try {
			TuiRunner runner = createTestRunner(
					terminalOut,
					KeyEvent.ofChar('n'),
					KeyEvent.ofChar('a'),
					KeyEvent.ofChar('m'),
					KeyEvent.ofChar('e'));

			Playground<JsonNode> pg = new Playground<>(
					env, Collections.singletonList(input), ".", JSON,
					RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
					false, false, System.out, System.err);

			pg.setEvaluationExecutor(command -> {
				backgroundExecutor.execute(() -> {
					try {
						command.run();
					} finally {
						if (pg.getQuery().equals(".name")) {
							finalTaskDone.countDown();
						}
					}
				});
			});

			Thread runThread = new Thread(() -> {
				try {
					pg.run(runner);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			runThread.start();

			assertThat(finalTaskDone.await(5, TimeUnit.SECONDS)).isTrue();
			runner.dispatch(TickEvent.of(2, Duration.ofMillis(250)));
			// Allow the render thread to apply the final queued evaluation result before exiting.
			Thread.sleep(150);
			runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
			runner.dispatch(KeyEvent.ofChar('y'));
			runThread.join(5000);

			assertThat(pg.getQuery()).isEqualTo(".name");
			assertThat(pg.getEvaluationStatus()).isEqualTo(Playground.EvaluationStatus.UP_TO_DATE);
			assertThat(String.join("\n", pg.getPreviewLines())).contains("\"Alice\"");
		} finally {
			backgroundExecutor.shutdownNow();
		}
	}

	@Test
	void evaluationResultRendersImmediatelyWithoutFurtherKeyEvents() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();

		CountDownLatch evalDone = new CountDownLatch(1);
		ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
		try {
			TuiRunner runner = createTestRunner(
					terminalOut,
					KeyEvent.ofChar('n'));

			Playground<JsonNode> pg = new Playground<>(
					env, Collections.singletonList(input), ".", JSON,
					RuntimeOptions.newBuilder().build(), CompileOptions.newBuilder().build(),
					false, false, System.out, System.err);

			pg.setEvaluationExecutor(command -> {
				backgroundExecutor.execute(() -> {
					try {
						command.run();
					} finally {
						evalDone.countDown();
					}
				});
			});

			Thread runThread = new Thread(() -> {
				try {
					pg.run(runner);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			runThread.start();

			assertThat(evalDone.await(5, TimeUnit.SECONDS)).isTrue();

			// Allow render thread to process UiRunnable and perform redraw
			Thread.sleep(150);

			String outputBeforeExit = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
			assertThat(outputBeforeExit).contains("✓ Up to date");
			assertThat(outputBeforeExit).doesNotContain("Output Preview (Stale)");

			runner.dispatch(KeyEvent.ofKey(KeyCode.ESCAPE));
			runner.dispatch(KeyEvent.ofChar('y'));
			runThread.join(5000);
		} finally {
			backgroundExecutor.shutdownNow();
		}
	}

	@Test
	void togglesNodeExpansionWithSpace() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> inner = new LinkedHashMap<>();
		inner.put("city", JSON.createString("Tokyo"));
		Map<String, JsonNode> root = new LinkedHashMap<>();
		root.put("address", JSON.createObject(inner));
		JsonNode input = JSON.createObject(root);

		// Focus INPUT (Tab twice), select address (Down), toggle with Space, submit
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofChar(' '),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		JsonTreeNode addressNode = pg.getInputTreePane().roots().get(0).children().get(0);
		assertThat(addressNode.key()).isEqualTo("address");
		assertThat(addressNode.isExpanded()).isFalse();
	}

	@Test
	void expandsAndCollapsesWithLeftRightAndHl() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> inner = new LinkedHashMap<>();
		inner.put("city", JSON.createString("Tokyo"));
		Map<String, JsonNode> root = new LinkedHashMap<>();
		root.put("address", JSON.createObject(inner));
		JsonNode input = JSON.createObject(root);

		// Focus INPUT (Tab twice), select address (Down), 'h' to collapse, 'l' to expand, submit
		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.TAB),
				KeyEvent.ofKey(KeyCode.DOWN),
				KeyEvent.ofChar('h'),
				KeyEvent.ofChar('l'),
				KeyEvent.ofKey(KeyCode.ESCAPE),
				KeyEvent.ofChar('y'));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		JsonTreeNode addressNode = pg.getInputTreePane().roots().get(0).children().get(0);
		assertThat(addressNode.isExpanded()).isTrue();
	}

	@Test
	void searchesAndNavigatesMatchesWithNAndP() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> user1 = new LinkedHashMap<>();
		user1.put("name", JSON.createString("Alice"));
		Map<String, JsonNode> user2 = new LinkedHashMap<>();
		user2.put("name", JSON.createString("Bob"));
		Map<String, JsonNode> user3 = new LinkedHashMap<>();
		user3.put("name", JSON.createString("Alice"));
		Map<String, JsonNode> root = new LinkedHashMap<>();
		root.put("users", JSON.createArray(Arrays.asList(JSON.createObject(user1), JSON.createObject(user2), JSON.createObject(user3))));
		JsonNode input = JSON.createObject(root);

		// Focus INPUT, search for 'Alice', navigate matches, then quit without clearing search state
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("Alice"));
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		events.add(KeyEvent.ofChar('n'));
		events.add(KeyEvent.ofChar('p'));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(pg.getInputTreePane().searchQuery()).isEqualTo("Alice");
		assertThat(pg.getInputTreePane().matches()).hasSize(2);
		assertThat(pg.getInputTreePane().matchIndex()).isEqualTo(0);
	}

	@Test
	void autoExpandsCollapsedAncestorsOnSearchMatch() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> secret = new LinkedHashMap<>();
		secret.put("token", JSON.createString("xyz123"));
		Map<String, JsonNode> profile = new LinkedHashMap<>();
		profile.put("secret", JSON.createObject(secret));
		Map<String, JsonNode> root = new LinkedHashMap<>();
		root.put("profile", JSON.createObject(profile));
		JsonNode input = JSON.createObject(root);

		// Focus INPUT (Tab twice), collapse profile ('h'), '/' search 'token', Enter, submit
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.DOWN)); // on profile
		events.add(KeyEvent.ofChar('h'));         // collapse profile
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("token"));
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		JsonTreeNode profileNode = pg.getInputTreePane().roots().get(0).children().get(0);
		assertThat(profileNode.key()).isEqualTo("profile");
		assertThat(profileNode.isExpanded()).isTrue();
		JsonTreeNode secretNode = profileNode.children().get(0);
		assertThat(secretNode.key()).isEqualTo("secret");
		assertThat(secretNode.isExpanded()).isTrue();
	}

	@Test
	void cancelsSearchInputWithEscape() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createString("test");

		// Focus INPUT, open search, cancel the search input with Escape, then submit with Escape
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("foo"));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getInputTreePane().isSearchActive()).isFalse();
		assertThat(pg.getInputTreePane().searchQuery()).isEmpty();
		assertThat(pg.getInputTreePane().searchBuffer()).isEmpty();
		assertThat(pg.getInputTreePane().matches()).isEmpty();
	}

	@Test
	void searchPatternHandlesReadlineEditingShortcuts() {
		JsonTreePane pane = new JsonTreePane();
		pane.setNodes(Collections.singletonList(JSON.createString("hello world")), Collections.singletonList("\"hello world\""), JSON);
		pane.handleKey(KeyEvent.ofChar('/'));
		assertThat(pane.isSearchActive()).isTrue();

		// Type "hello world"
		for (char c : "hello world".toCharArray()) {
			pane.handleKey(KeyEvent.ofChar(c));
		}
		assertThat(pane.searchBuffer()).isEqualTo("hello world");
		assertThat(pane.searchCursor()).isEqualTo(11);

		// Ctrl+A (Home)
		pane.handleKey(KeyEvent.ofChar('a', KeyModifiers.CTRL));
		assertThat(pane.searchCursor()).isEqualTo(0);

		// Ctrl+E (End)
		pane.handleKey(KeyEvent.ofChar('e', KeyModifiers.CTRL));
		assertThat(pane.searchCursor()).isEqualTo(11);

		// Ctrl+W (delete word backward -> "hello ")
		pane.handleKey(KeyEvent.ofChar('w', KeyModifiers.CTRL));
		assertThat(pane.searchBuffer()).isEqualTo("hello ");
		assertThat(pane.searchCursor()).isEqualTo(6);

		// Ctrl+H (backspace -> "hello")
		pane.handleKey(KeyEvent.ofChar('h', KeyModifiers.CTRL));
		assertThat(pane.searchBuffer()).isEqualTo("hello");
		assertThat(pane.searchCursor()).isEqualTo(5);

		// Left arrow twice (cursor at 3, between 'l' and 'l')
		pane.handleKey(KeyEvent.ofKey(KeyCode.LEFT));
		pane.handleKey(KeyEvent.ofKey(KeyCode.LEFT));
		assertThat(pane.searchCursor()).isEqualTo(3);

		// Ctrl+D (delete forward character -> "helo")
		pane.handleKey(KeyEvent.ofChar('d', KeyModifiers.CTRL));
		assertThat(pane.searchBuffer()).isEqualTo("helo");
		assertThat(pane.searchCursor()).isEqualTo(3);

		// Ctrl+K (kill to end -> "hel")
		pane.handleKey(KeyEvent.ofChar('k', KeyModifiers.CTRL));
		assertThat(pane.searchBuffer()).isEqualTo("hel");
		assertThat(pane.searchCursor()).isEqualTo(3);

		// Ctrl+U (kill to beginning -> "")
		pane.handleKey(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		assertThat(pane.searchBuffer()).isEmpty();
		assertThat(pane.searchCursor()).isEqualTo(0);
	}

	@Test
	void togglesBetweenTreeViewAndTextViewWithT() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createString("test");

		// Focus INPUT (Tab twice), press 't' to switch to TEXT mode
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('t'));
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);

		// Now press 't' twice to toggle to TEXT then back to TREE
		List<Event> events2 = new ArrayList<>();
		events2.add(KeyEvent.ofKey(KeyCode.TAB));
		events2.add(KeyEvent.ofKey(KeyCode.TAB));
		events2.add(KeyEvent.ofChar('t'));
		events2.add(KeyEvent.ofChar('t'));
		events2.add(KeyEvent.ofKey(KeyCode.ESCAPE));
		events2.add(KeyEvent.ofChar('y'));
		TuiRunner runner2 = createTestRunner(new ByteArrayOutputStream(), events2);
		Playground<JsonNode> pg2 = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));
		pg2.run(runner2);
		assertThat(pg2.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TREE);
	}

	@Test
	void searchesAndNavigatesMatchesInTextViewMode() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> obj = new LinkedHashMap<>();
		obj.put("first", JSON.createString("match"));
		obj.put("second", JSON.createString("match"));
		JsonNode input = JSON.createObject(obj);

		// Focus INPUT (Tab twice), toggle to TEXT ('t'), search '/' for 'match', navigate matches ('n')
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('t'));
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("match"));
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		events.add(KeyEvent.ofChar('n'));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(pg.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
		assertThat(pg.getInputTreePane().textMatches()).isNotEmpty();
		assertThat(pg.getInputTreePane().textMatchIndex()).isEqualTo(1);
	}

	@Test
	void preservesSearchQueryAcrossViewModeToggle() {
		JsonTreePane pane = new JsonTreePane();
		Map<String, JsonNode> obj = new LinkedHashMap<>();
		obj.put("foo", JSON.createString("bar"));
		JsonNode input = JSON.createObject(obj);
		pane.setNodes(Collections.singletonList(input), Collections.singletonList("{\"foo\": \"bar\"}"), JSON);

		pane.handleKey(KeyEvent.ofChar('/'));
		for (char c : "bar".toCharArray()) {
			pane.handleKey(KeyEvent.ofChar(c));
		}
		pane.handleKey(KeyEvent.ofKey(KeyCode.ENTER));

		assertThat(pane.matches()).isNotEmpty();
		assertThat(pane.matchIndex()).isEqualTo(0);

		// Toggle to TEXT mode
		pane.toggleViewMode();
		assertThat(pane.viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
		assertThat(pane.searchBuffer()).isEqualTo("bar");
		assertThat(pane.textMatches()).isNotEmpty();
		assertThat(pane.textMatchIndex()).isEqualTo(0);

		// Toggle back to TREE mode
		pane.toggleViewMode();
		assertThat(pane.viewMode()).isEqualTo(JsonTreePane.ViewMode.TREE);
		assertThat(pane.matches()).isNotEmpty();
		assertThat(pane.matchIndex()).isEqualTo(0);
	}

	@Test
	void ctrlDInSearchModeDeletesForwardWithoutSubmitting() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createString("test");

		// Focus INPUT (Tab twice), '/' search 'ab', Left arrow, Ctrl+D (delete 'b'), Enter, Esc (cancel/close search)
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("ab"));
		events.add(KeyEvent.ofKey(KeyCode.LEFT));
		events.add(KeyEvent.ofChar('d', KeyModifiers.CTRL)); // delete 'b'
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(pg.getInputTreePane().searchQuery()).isEqualTo("a");
	}

	@Test
	void retainsOutputTreeAndTextViewOnEvaluationError() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Map<String, JsonNode> obj = new LinkedHashMap<>();
		obj.put("foo", JSON.createString("bar"));
		JsonNode input = JSON.createObject(obj);

		// Type syntax error '[', Tab 3 times to focus OUTPUT, press 't' to toggle to TEXT mode,
		// press '/' and search 'bar', press Enter, then submit with Escape + y
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofChar('[')); // introduces syntax error in query
		events.add(KeyEvent.ofKey(KeyCode.TAB)); // focus DIAGNOSTICS
		events.add(KeyEvent.ofKey(KeyCode.TAB)); // focus INPUT
		events.add(KeyEvent.ofKey(KeyCode.TAB)); // focus OUTPUT
		events.add(KeyEvent.ofChar('t'));         // switch to TREE mode on stale output
		events.add(KeyEvent.ofChar('t'));         // switch back to TEXT mode on stale output
		events.add(KeyEvent.ofChar('/'));         // open search on stale output
		events.addAll(textToKeys("bar"));
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		events.add(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, 100, 24, events.toArray(new Event[0]));

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isFalse();
		assertThat(pg.getOutputTreePane().roots()).isNotEmpty();
		assertThat(pg.getOutputTreePane().roots().get(0).children().get(0).key()).isEqualTo("foo");
		assertThat(pg.getOutputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
		assertThat(pg.getOutputTreePane().textLines()).isNotEmpty();
		assertThat(pg.getOutputTreePane().textMatches()).isNotEmpty();
		assertThat(pg.getOutputTreePane().searchQuery()).isEqualTo("bar");
	}

	@Test
	void defaultsInputToTreeViewAndOutputToTextView() {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createString("test")),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		assertThat(pg.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TREE);
		assertThat(pg.getOutputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
	}

	@Test
	void backspacingOverSlashExitsSearchMode() {
		JsonTreePane pane = new JsonTreePane();
		pane.setNodes(Collections.singletonList(JSON.createString("hello")), Collections.singletonList("\"hello\""), JSON);

		// Open search with '/'
		pane.handleKey(KeyEvent.ofChar('/'));
		assertThat(pane.isSearchActive()).isTrue();

		// Press Backspace immediately: should delete '/' and exit search mode
		pane.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(pane.isSearchActive()).isFalse();
		assertThat(pane.searchBuffer()).isEmpty();

		// Open search with '/', type 'ab'
		pane.handleKey(KeyEvent.ofChar('/'));
		assertThat(pane.isSearchActive()).isTrue();
		pane.handleKey(KeyEvent.ofChar('a'));
		pane.handleKey(KeyEvent.ofChar('b'));
		assertThat(pane.searchBuffer()).isEqualTo("ab");

		// Backspace 'b' -> 'a'
		pane.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(pane.isSearchActive()).isTrue();
		assertThat(pane.searchBuffer()).isEqualTo("a");

		// Backspace 'a' -> empty
		pane.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(pane.isSearchActive()).isTrue();
		assertThat(pane.searchBuffer()).isEmpty();

		// Backspace over '/' -> exit search mode
		pane.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(pane.isSearchActive()).isFalse();
		assertThat(pane.searchQuery()).isEmpty();

		// Test Ctrl+H also exits when cursor is at 0
		pane.handleKey(KeyEvent.ofChar('/'));
		assertThat(pane.isSearchActive()).isTrue();
		pane.handleKey(KeyEvent.ofChar('h', KeyModifiers.CTRL));
		assertThat(pane.isSearchActive()).isFalse();
	}

	@Test
	void pressingSlashAgainStartsSearchFromEmpty() {
		JsonTreePane pane = new JsonTreePane();
		pane.setNodes(Collections.singletonList(JSON.createString("hello")), Collections.singletonList("\"hello\""), JSON);

		// First search: /hello<ENTER>
		pane.handleKey(KeyEvent.ofChar('/'));
		for (char c : "hello".toCharArray()) {
			pane.handleKey(KeyEvent.ofChar(c));
		}
		pane.handleKey(KeyEvent.ofKey(KeyCode.ENTER));

		assertThat(pane.isSearchActive()).isFalse();
		assertThat(pane.searchQuery()).isEqualTo("hello");
		assertThat(pane.matches()).isNotEmpty();

		// Press '/' again -> should start from clean empty state
		pane.handleKey(KeyEvent.ofChar('/'));
		assertThat(pane.isSearchActive()).isTrue();
		assertThat(pane.searchBuffer()).isEmpty();
		assertThat(pane.searchQuery()).isEmpty();
		assertThat(pane.matches()).isEmpty();
	}

	@Test
	void pressingEscapeAfterSearchClearsSearchModeWithoutConfirmQuitModal() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		JsonNode input = JSON.createObject(Collections.singletonMap("name", JSON.createString("Alice")));

		// Focus INPUT (Tab twice), commit a search, clear it with Escape, cancel submit once, then submit
		List<Event> events = new ArrayList<>();
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofKey(KeyCode.TAB));
		events.add(KeyEvent.ofChar('/'));
		events.addAll(textToKeys("Alice"));
		events.add(KeyEvent.ofKey(KeyCode.ENTER));
		// At this point, search is committed and active with query "Alice"
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE)); // Should clear search without showing confirm quit modal
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE)); // Should show submit confirmation modal
		events.add(KeyEvent.ofChar('n')); // Cancel submit modal
		events.add(KeyEvent.ofKey(KeyCode.ESCAPE)); // Submit
		events.add(KeyEvent.ofChar('y'));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		TuiRunner runner = createTestRunner(terminalOut, events);

		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(input),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		pg.run(runner);

		assertThat(pg.isAccepted()).isTrue();
		assertThat(pg.getInputTreePane().searchQuery()).isEmpty();
		assertThat(pg.getInputTreePane().matches()).isEmpty();
		String outputText = new String(terminalOut.toByteArray(), StandardCharsets.UTF_8);
		assertThat(outputText).contains("(n/p/Esc)");
		assertThat(outputText).doesNotContain("(n/p/t/Esc)");
	}

	@Test
	void escapeInPaneClearsSearchDirectly() {
		JsonTreePane pane = new JsonTreePane();
		pane.setNodes(Collections.singletonList(JSON.createString("hello")), Collections.singletonList("\"hello\""), JSON);

		pane.handleKey(KeyEvent.ofChar('/'));
		for (char c : "hello".toCharArray()) {
			pane.handleKey(KeyEvent.ofChar(c));
		}
		pane.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(pane.searchQuery()).isEqualTo("hello");
		assertThat(pane.matches()).isNotEmpty();

		// ESC in normal mode with active search query clears search
		boolean consumed = pane.handleKey(KeyEvent.ofKey(KeyCode.ESCAPE));
		assertThat(consumed).isTrue();
		assertThat(pane.searchQuery()).isEmpty();
		assertThat(pane.matches()).isEmpty();

		// ESC with no search query does not consume key
		boolean consumedAgain = pane.handleKey(KeyEvent.ofKey(KeyCode.ESCAPE));
		assertThat(consumedAgain).isFalse();
	}

	@Test
	void escapeInSearchInputCancelsSearchInsteadOfCommitting() {
		JsonTreePane pane = new JsonTreePane();
		pane.setNodes(Collections.singletonList(JSON.createString("hello")), Collections.singletonList("\"hello\""), JSON);

		pane.handleKey(KeyEvent.ofChar('/'));
		for (char c : "hello".toCharArray()) {
			pane.handleKey(KeyEvent.ofChar(c));
		}
		assertThat(pane.isSearchActive()).isTrue();
		assertThat(pane.matches()).isNotEmpty();

		// ESC while typing discards the query instead of committing it like ENTER does
		boolean consumed = pane.handleKey(KeyEvent.ofKey(KeyCode.ESCAPE));
		assertThat(consumed).isTrue();
		assertThat(pane.isSearchActive()).isFalse();
		assertThat(pane.searchQuery()).isEmpty();
		assertThat(pane.searchBuffer()).isEmpty();
		assertThat(pane.matches()).isEmpty();
		assertThat(pane.textMatches()).isEmpty();
	}

	@Test
	void guideLineContextualRendering() {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createObject(Collections.singletonMap("a", JSON.createString("val")))),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		// 1. QUERY guide
		String queryGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.QUERY));
		assertThat(queryGuide).contains("Tab Focus Next");
		assertThat(queryGuide).contains("Ctrl+R Run Query");
		assertThat(queryGuide).contains("Esc Emit & Quit");
		assertThat(queryGuide).contains("Ctrl+C Quit");
		assertThat(queryGuide).doesNotContain("Ctrl+P");
		assertThat(queryGuide).doesNotContain("Ctrl+U/Ctrl+D Page Up/Down");
		assertThat(queryGuide).doesNotContain("Ctrl+D Down");
		assertThat(queryGuide).doesNotContain("Ctrl+U Up");
		assertThat(queryGuide).doesNotContain("Evaluate");
		assertThat(queryGuide).doesNotContain("Diagnostics");

		// 2. DIAGNOSTICS guide
		String diagGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.DIAGNOSTICS));
		assertThat(diagGuide).contains("Scroll");
		assertThat(diagGuide).contains("Tab Focus Next");
		assertThat(diagGuide).contains("Ctrl+R Run Query");
		assertThat(diagGuide).contains("Esc Emit & Quit");
		assertThat(diagGuide).contains("Ctrl+C Quit");
		assertThat(diagGuide).doesNotContain("Ctrl+P");
		assertThat(diagGuide).doesNotContain("Ctrl+U/Ctrl+D Page Up/Down");
		assertThat(diagGuide).doesNotContain("Ctrl+D Down");
		assertThat(diagGuide).doesNotContain("Ctrl+U Up");
		assertThat(diagGuide).doesNotContain("Evaluate");
		assertThat(diagGuide).doesNotContain("Accept");
		assertThat(diagGuide).doesNotContain("Input");

		// 3. INPUT pane in default Tree view, no active search
		assertThat(pg.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TREE);
		String inputTreeGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.INPUT));
		assertThat(inputTreeGuide).contains("Collapse/Expand");
		assertThat(inputTreeGuide).contains("Space Toggle");
		assertThat(inputTreeGuide).contains("t Toggle View");
		assertThat(inputTreeGuide).contains("Tab Focus Next");
		assertThat(inputTreeGuide).contains("Ctrl+U/Ctrl+D Page Up/Down");
		assertThat(inputTreeGuide).contains("Ctrl+R Run Query");
		assertThat(inputTreeGuide).contains("Esc Emit & Quit");
		assertThat(inputTreeGuide).contains("Ctrl+C Quit");
		assertThat(inputTreeGuide).doesNotContain("Ctrl+P");
		assertThat(inputTreeGuide).doesNotContain("Ctrl+D Down", "Ctrl+U Up");
		assertThat(inputTreeGuide).doesNotContain("Accept");
		assertThat(inputTreeGuide).doesNotContain("t View");
		assertThat(inputTreeGuide).doesNotContain("Tab Output");
		assertThat(inputTreeGuide).doesNotContain("Matches");

		// 4. INPUT pane in Text view, no active search
		pg.getInputTreePane().toggleViewMode();
		assertThat(pg.getInputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
		String inputTextGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.INPUT));
		assertThat(inputTextGuide).doesNotContain("Collapse/Expand");
		assertThat(inputTextGuide).doesNotContain("Space Toggle");
		assertThat(inputTextGuide).contains("t Toggle View");
		assertThat(inputTextGuide).contains("Tab Focus Next");
		assertThat(inputTextGuide).contains("Ctrl+U/Ctrl+D Page Up/Down");
		assertThat(inputTextGuide).contains("Ctrl+R Run Query");
		assertThat(inputTextGuide).contains("Esc Emit & Quit");
		assertThat(inputTextGuide).contains("Ctrl+C Quit");
		assertThat(inputTextGuide).doesNotContain("Ctrl+P");
		assertThat(inputTextGuide).doesNotContain("Ctrl+D Down", "Ctrl+U Up");
		assertThat(inputTextGuide).doesNotContain("Accept");
		assertThat(inputTextGuide).doesNotContain("Matches");

		// 5. INPUT pane while a search query is being typed
		pg.getInputTreePane().handleKey(KeyEvent.ofChar('/'));
		pg.getInputTreePane().handleKey(KeyEvent.ofChar('v'));
		assertThat(pg.getInputTreePane().isSearchActive()).isTrue();
		String inputSearchTypingGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.INPUT));
		assertThat(inputSearchTypingGuide).doesNotContain("Esc Emit & Quit");
		assertThat(inputSearchTypingGuide).doesNotContain("Matches");

		// 6. INPUT pane with the search query committed
		pg.getInputTreePane().handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		String inputSearchGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.INPUT));
		assertThat(inputSearchGuide).doesNotContain("Esc Emit & Quit");
		assertThat(inputSearchGuide).doesNotContain("Matches");

		// 7. INPUT pane after search is cleared
		pg.getInputTreePane().clearSearch();
		String inputSearchClearedGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.INPUT));
		assertThat(inputSearchClearedGuide).contains("Esc Emit & Quit");
		assertThat(inputSearchClearedGuide).doesNotContain("Matches");

		// 8. OUTPUT pane in default Text view, no active search
		assertThat(pg.getOutputTreePane().viewMode()).isEqualTo(JsonTreePane.ViewMode.TEXT);
		String outputTextGuide = lineToPlainText(pg.buildGuideLine(Playground.Focus.OUTPUT));
		assertThat(outputTextGuide).doesNotContain("Collapse/Expand");
		assertThat(outputTextGuide).doesNotContain("Space Toggle");
		assertThat(outputTextGuide).contains("t Toggle View");
		assertThat(outputTextGuide).contains("Tab Focus Next");
		assertThat(outputTextGuide).contains("Ctrl+U/Ctrl+D Page Up/Down");
		assertThat(outputTextGuide).contains("Ctrl+R Run Query");
		assertThat(outputTextGuide).contains("Esc Emit & Quit");
		assertThat(outputTextGuide).contains("Ctrl+C Quit");
		assertThat(outputTextGuide).doesNotContain("Ctrl+P");
		assertThat(outputTextGuide).doesNotContain("Ctrl+D Down", "Ctrl+U Up");
		assertThat(outputTextGuide).doesNotContain("Accept");
		assertThat(outputTextGuide).doesNotContain("t View");
		assertThat(outputTextGuide).doesNotContain("Tab Query");
		assertThat(outputTextGuide).doesNotContain("Matches");
	}

	@Test
	void stylesAutoRunToggleHintAsDim() {
		Playground<JsonNode> pg = new Playground<>(
				Main.createEnvironment(JSON, Versions.JQ_1_6),
				Collections.singletonList(JSON.createNull()),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		Line statusLine = pg.buildAutoRunStatusLine();
		assertThat(lineToPlainText(statusLine)).isEqualTo(" Auto-run: On (Ctrl+P to toggle) ");
		assertThat(statusLine.spans()).hasSize(2);
		assertThat(statusLine.spans().get(1).content()).isEqualTo(" (Ctrl+P to toggle) ");
		assertThat(statusLine.spans().get(1).style()).isEqualTo(Style.EMPTY.dim());
	}

	@Test
	void enterAndLegacyShortcutsDoNotSubmitInNonQueryPanes() throws Exception {
		Environment<JsonNode> env = Main.createEnvironment(JSON, Versions.JQ_1_6);
		Playground<JsonNode> pg = new Playground<>(
				env,
				Collections.singletonList(JSON.createObject(Collections.singletonMap("key", JSON.createString("value")))),
				".",
				JSON,
				RuntimeOptions.newBuilder().build(),
				CompileOptions.newBuilder().build(),
				false,
				false,
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(new ByteArrayOutputStream()));

		ByteArrayOutputStream terminalOut = new ByteArrayOutputStream();
		// Test sequence:
		// Tab -> DIAGNOSTICS: press ENTER -> modal remains NONE
		// Tab -> INPUT: press ENTER -> modal remains NONE
		// Tab -> OUTPUT: press ENTER -> modal remains NONE
		// Press Ctrl+D -> modal remains NONE
		// Press Ctrl+S -> modal remains NONE
		// Press Esc -> modal becomes CONFIRM_SUBMIT
		// Press 'y' -> accepted and quit
		TuiRunner runner = createTestRunner(
				terminalOut,
				KeyEvent.ofKey(KeyCode.TAB), // -> DIAGNOSTICS
				KeyEvent.ofKey(KeyCode.ENTER),
				KeyEvent.ofKey(KeyCode.TAB), // -> INPUT
				KeyEvent.ofKey(KeyCode.ENTER),
				KeyEvent.ofKey(KeyCode.TAB), // -> OUTPUT
				KeyEvent.ofKey(KeyCode.ENTER),
				KeyEvent.ofChar('d', KeyModifiers.CTRL), // Ctrl+D does nothing
				KeyEvent.ofChar('s', KeyModifiers.CTRL), // Ctrl+S does nothing
				KeyEvent.ofKey(KeyCode.ESCAPE), // Esc submits
				KeyEvent.ofChar('y')); // Confirm

		pg.run(runner);
		assertThat(pg.isAccepted()).isTrue();
	}

	private static String lineToPlainText(Line line) {
		StringBuilder sb = new StringBuilder();
		for (Span span : line.spans()) {
			sb.append(span.content());
		}
		return sb.toString();
	}

	private static List<Event> textToKeys(String text) {
		List<Event> keys = new ArrayList<>();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				keys.add(KeyEvent.ofKey(KeyCode.ENTER));
			} else if (c == '\t') {
				keys.add(KeyEvent.ofKey(KeyCode.TAB));
			} else {
				keys.add(KeyEvent.ofChar(c));
			}
		}
		return keys;
	}

	private static TuiRunner createTestRunner(ByteArrayOutputStream terminalOut, int width, int height, Event... events) throws Exception {
		LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", terminalOut, StandardCharsets.UTF_8);
		terminal.setSize(new Size(width, height));
		TuiRunner runner = TuiRunner.create(TuiConfig.builder()
				.rawMode(false)
				.shutdownHook(false)
				.alternateScreen(false)
				.hideCursor(false)
				.backend(new JLineBackend(terminal))
				.build());
		for (Event event : events) {
			runner.dispatch(event);
		}
		return runner;
	}

	private static TuiRunner createTestRunner(ByteArrayOutputStream terminalOut, Event... events) throws Exception {
		return createTestRunner(terminalOut, 80, 24, events);
	}

	private static TuiRunner createTestRunner(ByteArrayOutputStream terminalOut, List<Event> events) throws Exception {
		return createTestRunner(terminalOut, events.toArray(new Event[0]));
	}
}
