package net.thisptr.jackson.jq.v2.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import com.google.errorprone.annotations.Var;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.input.TextArea;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.widgets.spinner.Spinner;
import dev.tamboui.widgets.spinner.SpinnerState;
import dev.tamboui.widgets.spinner.SpinnerStyle;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class Playground<N> {
	private static final String PRETTY_INDENT = "  ";
	private static final int PAGE_SCROLL = 15;
	private static final int MAX_STRING_LENGTH_OPTION = 6;
	private static final int MAX_BINARY_LENGTH_OPTION = 7;
	private static final int MAX_ARRAY_LENGTH_OPTION = 8;
	private static final int MAX_OBJECT_MEMBER_COUNT_OPTION = 9;
	private static final int MAX_USER_DEFINED_FUNCTION_CALLS_OPTION = 10;
	private static final int MAX_OUTPUTS_PER_EXPRESSION_OPTION = 11;
	private static final int FIRST_RUNTIME_LIMIT_OPTION = MAX_STRING_LENGTH_OPTION;
	private static final int OPTION_COUNT = MAX_OUTPUTS_PER_EXPRESSION_OPTION + 1;
	private static final int LIMIT_FLAG_WIDTH = 35;
	private static final int LIMIT_VALUE_WIDTH = 20;

	enum Focus {
		QUERY,
		DIAGNOSTICS,
		INPUT,
		OUTPUT
	}

	enum Modal {
		NONE,
		CONFIRM_QUIT,
		CONFIRM_SUBMIT,
		OPTIONS
	}

	enum EvaluationStatus {
		UP_TO_DATE,
		STALE,
		FAILURE,
		LOADING
	}

	private Environment<?> env;
	private Version version;
	private String providerName;
	private JsonProvider<?> jsonProvider;
	private final byte @Nullable [] rawInputBytes;
	private final boolean nullInput;
	private boolean rawInput;
	private boolean slurp;
	private RuntimeOptions runtimeOptions;
	private final CompileOptions compileOptions;
	private boolean compact;
	private boolean rawOutput;
	private final boolean warningsEnabled;
	private final List<String> inputFiles;
	private final PrintStream out;
	private final PrintStream err;

	private final TextAreaState queryState;
	private final JsonTreePane inputTreePane = new JsonTreePane(JsonTreePane.ViewMode.TREE);
	private final JsonTreePane outputTreePane = new JsonTreePane(JsonTreePane.ViewMode.TEXT);
	private List<?> inputs = new ArrayList<>();
	private List<String> inputLines = Collections.emptyList();
	private @Nullable String inputErrorMessage;
	private Focus focus = Focus.QUERY;
	private Modal modal = Modal.NONE;
	private int selectedOptionIndex = 0;
	private int inputViewportHeight = 1;
	private int outputViewportHeight = 1;
	private int diagnosticsViewportHeight = 1;
	private int inputScrollOffset;
	private int outputScrollOffset;
	private int diagnosticsScrollOffset;
	private volatile List<String> previewLines = new ArrayList<>();
	private volatile @Nullable String errorMessage;
	private List<Diagnostic> warnings = new ArrayList<>();
	private List<Line> diagnosticLines = Collections.emptyList();
	private List<String> diagnosticPlainLines = Collections.emptyList();
	private int itemCount;
	private boolean automaticEvaluationPaused;
	private volatile boolean outputStale;
	private boolean accepted;
	private volatile EvaluationStatus evaluationStatus = EvaluationStatus.UP_TO_DATE;
	private volatile boolean redrawRequested;
	private final SpinnerState spinnerState = new SpinnerState();
	private @Nullable Executor evaluationExecutor;
	private volatile long evaluationVersion;
	private @Nullable TuiRunner runner;

	Playground(Environment<?> env, Version version, String providerName, byte @Nullable [] rawInputBytes,
			   boolean nullInput, boolean rawInput, boolean slurp,
			   String initialQuery, JsonProvider<?> jsonProvider,
			   RuntimeOptions runtimeOptions, CompileOptions compileOptions, boolean compact, boolean rawOutput,
			   boolean warningsEnabled, PrintStream out, PrintStream err) {
		this(env, version, providerName, rawInputBytes, nullInput, rawInput, slurp, initialQuery, jsonProvider,
				runtimeOptions, compileOptions, compact, rawOutput, warningsEnabled, Collections.emptyList(), out, err);
	}

	Playground(Environment<?> env, Version version, String providerName, byte @Nullable [] rawInputBytes,
			   boolean nullInput, boolean rawInput, boolean slurp,
			   String initialQuery, JsonProvider<?> jsonProvider,
			   RuntimeOptions runtimeOptions, CompileOptions compileOptions, boolean compact, boolean rawOutput,
			   boolean warningsEnabled, List<String> inputFiles, PrintStream out, PrintStream err) {
		this.env = env;
		this.version = version;
		this.providerName = providerName;
		this.jsonProvider = jsonProvider;
		this.rawInputBytes = rawInputBytes;
		this.nullInput = nullInput;
		this.rawInput = rawInput;
		this.slurp = slurp;
		this.runtimeOptions = runtimeOptions;
		this.compileOptions = compileOptions;
		this.compact = compact;
		this.rawOutput = rawOutput;
		this.warningsEnabled = warningsEnabled;
		this.inputFiles = Collections.unmodifiableList(new ArrayList<>(inputFiles));
		this.out = out;
		this.err = err;
		this.queryState = new TextAreaState(initialQuery);
		this.queryState.moveCursorToEnd();

		updateInputs();
	}

	Playground(Environment<N> env, byte @Nullable [] rawInputBytes, boolean nullInput, boolean rawInput, boolean slurp,
			   String initialQuery, JsonProvider<N> jsonProvider,
			   RuntimeOptions runtimeOptions, CompileOptions compileOptions, boolean compact, boolean rawOutput,
			   boolean warningsEnabled, PrintStream out, PrintStream err) {
		this(env, Versions.JQ_1_6, Main.resolveProviderName(jsonProvider), rawInputBytes, nullInput, rawInput, slurp,
				initialQuery, jsonProvider, runtimeOptions, compileOptions, compact, rawOutput, warningsEnabled, out, err);
	}

	Playground(Environment<N> env, List<N> inputs, String initialQuery, JsonProvider<N> jsonProvider,
			   RuntimeOptions runtimeOptions, CompileOptions compileOptions, boolean compact, boolean rawOutput,
			   boolean warningsEnabled, PrintStream out, PrintStream err) {
		this(env, serializeInputs(inputs, jsonProvider), false, false, false, initialQuery, jsonProvider,
				runtimeOptions, compileOptions, compact, rawOutput, warningsEnabled, out, err);
	}

	Playground(Environment<N> env, List<N> inputs, String initialQuery, JsonProvider<N> jsonProvider,
			   RuntimeOptions runtimeOptions, CompileOptions compileOptions, boolean compact, boolean rawOutput,
			   PrintStream out, PrintStream err) {
		this(env, inputs, initialQuery, jsonProvider, runtimeOptions, compileOptions, compact, rawOutput, true, out, err);
	}

	private static <N> byte[] serializeInputs(List<N> inputs, JsonProvider<N> jsonProvider) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (N in : inputs) {
			try {
				baos.write(jsonProvider.format(in).getBytes(StandardCharsets.UTF_8));
				baos.write('\n');
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return baos.toByteArray();
	}

	private void updateInputs() {
		updateInputsGeneric(jsonProvider);
	}

	private <T> void updateInputsGeneric(JsonProvider<T> provider) {
		List<T> newInputs = new ArrayList<>();
		inputErrorMessage = null;
		if (nullInput) {
			newInputs.add(provider.createNull());
		} else if (rawInputBytes != null) {
			try {
				InputSource<T> source = InputSources.create(
						provider,
						Collections.singletonList(new ByteArrayInputStream(rawInputBytes)),
						false,
						rawInput,
						slurp);
				source.readAll(newInputs::add);
			} catch (Exception e) {
				inputErrorMessage = e.getMessage();
			}
		}
		this.inputs = newInputs;
		updateInputLines();
		updateEvaluation();
	}

	private void updateInputLines() {
		updateInputLinesGeneric(jsonProvider, inputs);
	}

	// Safe unchecked cast: inputs always holds node instances created with the matching jsonProvider.
	@SuppressWarnings("unchecked")
	private <T> void updateInputLinesGeneric(JsonProvider<T> provider, List<?> inList) {
		List<String> inLines = new ArrayList<>();
		if (inputErrorMessage != null) {
			inLines.add("(error parsing input: " + inputErrorMessage + ")");
		} else if (inList.isEmpty()) {
			inLines.add("(no input)");
		} else {
			for (Object in : inList) {
				String formatted = JqPrettyPrinter.print(provider, (T) in, PRETTY_INDENT);
				for (String line : formatted.split("\r?\n", -1)) {
					inLines.add(line);
				}
			}
		}
		this.inputLines = Collections.unmodifiableList(inLines);
		this.inputScrollOffset = 0;
		this.inputTreePane.setNodes(inList, this.inputLines, provider);
	}

	private void cycleVersion(int direction) {
		List<Version> versions = Versions.versions();
		int currentIdx = Math.max(0, versions.indexOf(version));
		int next = (currentIdx + direction + versions.size()) % versions.size();
		this.version = versions.get(next);
		this.env = Main.createEnvironment(jsonProvider, this.version);
		updateEvaluation();
	}

	private void cycleProvider(int direction) {
		List<String> providers = Main.PROVIDERS;
		int currentIdx = Math.max(0, providers.indexOf(providerName));
		int next = (currentIdx + direction + providers.size()) % providers.size();
		this.providerName = providers.get(next);
		this.jsonProvider = Main.resolveProvider(this.providerName);
		this.env = Main.createEnvironment(this.jsonProvider, this.version);
		updateInputs();
	}

	private void toggleOption(int index) {
		switch (index) {
			case 0:
				rawInput = !rawInput;
				updateInputs();
				break;
			case 1:
				slurp = !slurp;
				updateInputs();
				break;
			case 2:
				compact = !compact;
				updateEvaluation();
				break;
			case 3:
				rawOutput = !rawOutput;
				updateEvaluation();
				break;
			case 4:
				cycleVersion(1);
				break;
			case 5:
				cycleProvider(1);
				break;
		}
	}

	private static boolean isRuntimeLimitOption(int index) {
		return index >= FIRST_RUNTIME_LIMIT_OPTION && index < OPTION_COUNT;
	}

	private void appendRuntimeLimitDigit(int index, int digit) {
		long current = getRuntimeLimit(index);
		long maximum = getRuntimeLimitMaximum(index);
		if (current == maximum) {
			setRuntimeLimit(index, digit);
			return;
		}
		if (current <= (maximum - digit) / 10) {
			setRuntimeLimit(index, current * 10 + digit);
		}
	}

	private void deleteRuntimeLimitDigit(int index) {
		long current = getRuntimeLimit(index);
		long maximum = getRuntimeLimitMaximum(index);
		if (current == maximum) {
			return;
		}
		setRuntimeLimit(index, current == 0 ? maximum : current / 10);
	}

	private static long getRuntimeLimitMaximum(int index) {
		return index <= MAX_OBJECT_MEMBER_COUNT_OPTION ? Integer.MAX_VALUE : Long.MAX_VALUE;
	}

	private long getRuntimeLimit(int index) {
		return switch (index) {
			case MAX_STRING_LENGTH_OPTION -> runtimeOptions.getMaxStringLength();
			case MAX_BINARY_LENGTH_OPTION -> runtimeOptions.getMaxBinaryLength();
			case MAX_ARRAY_LENGTH_OPTION -> runtimeOptions.getMaxArrayLength();
			case MAX_OBJECT_MEMBER_COUNT_OPTION -> runtimeOptions.getMaxObjectMemberCount();
			case MAX_USER_DEFINED_FUNCTION_CALLS_OPTION -> runtimeOptions.getMaxUserDefinedFunctionCalls();
			case MAX_OUTPUTS_PER_EXPRESSION_OPTION -> runtimeOptions.getMaxOutputsPerExpression();
			default -> throw new IllegalArgumentException("not a runtime limit option: " + index);
		};
	}

	private void setRuntimeLimit(int index, long value) {
		RuntimeOptions.Builder builder = RuntimeOptions.newBuilder()
				.setMaxStringLength(runtimeOptions.getMaxStringLength())
				.setMaxBinaryLength(runtimeOptions.getMaxBinaryLength())
				.setMaxArrayLength(runtimeOptions.getMaxArrayLength())
				.setMaxObjectMemberCount(runtimeOptions.getMaxObjectMemberCount())
				.setMaxUserDefinedFunctionCalls(runtimeOptions.getMaxUserDefinedFunctionCalls())
				.setMaxOutputsPerExpression(runtimeOptions.getMaxOutputsPerExpression());

		switch (index) {
			case MAX_STRING_LENGTH_OPTION:
				builder.setMaxStringLength((int) value);
				break;
			case MAX_BINARY_LENGTH_OPTION:
				builder.setMaxBinaryLength((int) value);
				break;
			case MAX_ARRAY_LENGTH_OPTION:
				builder.setMaxArrayLength((int) value);
				break;
			case MAX_OBJECT_MEMBER_COUNT_OPTION:
				builder.setMaxObjectMemberCount((int) value);
				break;
			case MAX_USER_DEFINED_FUNCTION_CALLS_OPTION:
				builder.setMaxUserDefinedFunctionCalls(value);
				break;
			case MAX_OUTPUTS_PER_EXPRESSION_OPTION:
				builder.setMaxOutputsPerExpression(value);
				break;
			default:
				throw new IllegalArgumentException("not a runtime limit option: " + index);
		}

		runtimeOptions = builder.build();
		updateEvaluation();
	}

	void run(TuiRunner runner) throws Exception {
		try (TuiRunner r = runner) {
			this.runner = r;
			r.run(this::handleEvent, this::render);
		} finally {
			this.runner = null;
		}
		if (accepted) {
			emitResults();
		} else {
			err.println(finalCommand());
		}
	}

	private boolean handleEvent(Event event, TuiRunner runner) {
		if (event instanceof TickEvent) {
			if (evaluationStatus == EvaluationStatus.LOADING) {
				spinnerState.advance();
				return true;
			}
			if (redrawRequested) {
				redrawRequested = false;
				return true;
			}
			return false;
		}
		if (!(event instanceof KeyEvent key)) {
			return false;
		}
		if (modal != Modal.NONE && key.hasCtrl() && (key.isChar('p') || key.isChar('r'))) {
			return true;
		}

		// Modal dialog active
		if (modal == Modal.OPTIONS) {
			if (key.isCtrlC()) {
				modal = Modal.CONFIRM_QUIT;
				return true;
			}
			if (key.isUp() || key.isChar('k')) {
				selectedOptionIndex = (selectedOptionIndex + OPTION_COUNT - 1) % OPTION_COUNT;
				return true;
			}
			if (key.isDown() || key.isChar('j')) {
				selectedOptionIndex = (selectedOptionIndex + 1) % OPTION_COUNT;
				return true;
			}
			if (key.isLeft() || key.isChar('h')) {
				if (selectedOptionIndex == 4) {
					cycleVersion(-1);
					return true;
				}
				if (selectedOptionIndex == 5) {
					cycleProvider(-1);
					return true;
				}
			}
			if (key.isRight() || key.isChar('l')) {
				if (selectedOptionIndex == 4) {
					cycleVersion(1);
					return true;
				}
				if (selectedOptionIndex == 5) {
					cycleProvider(1);
					return true;
				}
			}
			if (key.isChar(' ')) {
				toggleOption(selectedOptionIndex);
				return true;
			}
			if (key.isChar('R')) {
				toggleOption(0);
				return true;
			}
			if (key.isChar('s') || key.isChar('S')) {
				toggleOption(1);
				return true;
			}
			if (key.isChar('c') || key.isChar('C')) {
				toggleOption(2);
				return true;
			}
			if (key.isChar('r')) {
				toggleOption(3);
				return true;
			}
			if (key.isChar('v')) {
				cycleVersion(1);
				return true;
			}
			if (key.isChar('V')) {
				cycleVersion(-1);
				return true;
			}
			if (key.isChar('p')) {
				cycleProvider(1);
				return true;
			}
			if (key.isChar('P')) {
				cycleProvider(-1);
				return true;
			}
			if (isRuntimeLimitOption(selectedOptionIndex)) {
				if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE) {
					deleteRuntimeLimitDigit(selectedOptionIndex);
					return true;
				}
				if (!key.hasCtrl() && !key.hasAlt()) {
					String str = key.string();
					if (str != null && str.length() == 1 && str.charAt(0) >= '0' && str.charAt(0) <= '9') {
						appendRuntimeLimitDigit(selectedOptionIndex, str.charAt(0) - '0');
						return true;
					}
				}
			}
			if (key.code() == KeyCode.ESCAPE || key.code() == KeyCode.ENTER || key.isConfirm() || key.isCancel()
					|| key.isCharIgnoreCase('q') || (key.hasCtrl() && key.isChar('o'))) {
				modal = Modal.NONE;
				return true;
			}
			return true;
		}
		if (modal != Modal.NONE) {
			if (key.isCharIgnoreCase('y')) {
				accepted = (modal == Modal.CONFIRM_SUBMIT);
				runner.quit();
				return true;
			}
			if (modal == Modal.CONFIRM_SUBMIT && (key.isConfirm() || key.code() == KeyCode.ENTER)) {
				accepted = true;
				runner.quit();
				return true;
			}
			if (key.isCharIgnoreCase('n') || key.code() == KeyCode.ESCAPE || key.isCancel() || key.isCtrlC()) {
				modal = Modal.NONE;
				return true;
			}
			return false;
		}

		// Automatic evaluation controls
		if (key.hasCtrl() && key.isChar('p')) {
			automaticEvaluationPaused = !automaticEvaluationPaused;
			if (!automaticEvaluationPaused) {
				evaluateNow();
			}
			return true;
		}
		if (key.hasCtrl() && key.isChar('r')) {
			evaluateNow();
			return true;
		}
		if (key.isCtrlC()) {
			modal = Modal.CONFIRM_QUIT;
			return true;
		}

		if (focus == Focus.INPUT && inputTreePane.isSearchActive()) {
			if (key.isFocusPrevious() || key.isFocusNext() || key.code() == KeyCode.TAB) {
				inputTreePane.clearSearch();
			} else {
				return inputTreePane.handleKey(key);
			}
		}
		if (focus == Focus.OUTPUT && outputTreePane.isSearchActive()) {
			if (key.isFocusPrevious() || key.isFocusNext() || key.code() == KeyCode.TAB) {
				outputTreePane.clearSearch();
			} else {
				return outputTreePane.handleKey(key);
			}
		}

		// Options dialog trigger (Ctrl+O)
		if (key.hasCtrl() && key.isChar('o')) {
			modal = Modal.OPTIONS;
			return true;
		}

		// Emit and quit trigger (Escape)
		if (key.isCancel() || key.code() == KeyCode.ESCAPE) {
			if (focus == Focus.INPUT && !inputTreePane.searchQuery().isEmpty()) {
				inputTreePane.clearSearch();
				return true;
			}
			if (focus == Focus.OUTPUT && !outputTreePane.searchQuery().isEmpty()) {
				outputTreePane.clearSearch();
				return true;
			}
			modal = Modal.CONFIRM_SUBMIT;
			return true;
		}

		// Focus switching (Tab or Shift+Tab)
		if (key.isFocusPrevious() || (key.code() == KeyCode.TAB && key.hasShift())) {
			if (focus == Focus.INPUT && inputTreePane.isSearchActive()) {
				inputTreePane.clearSearch();
			}
			if (focus == Focus.OUTPUT && outputTreePane.isSearchActive()) {
				outputTreePane.clearSearch();
			}
			if (focus == Focus.QUERY) {
				focus = Focus.OUTPUT;
			} else if (focus == Focus.OUTPUT) {
				focus = Focus.INPUT;
			} else if (focus == Focus.INPUT) {
				focus = Focus.DIAGNOSTICS;
			} else {
				focus = Focus.QUERY;
			}
			return true;
		}
		if (key.code() == KeyCode.TAB || key.isFocusNext()) {
			if (focus == Focus.INPUT && inputTreePane.isSearchActive()) {
				inputTreePane.clearSearch();
			}
			if (focus == Focus.OUTPUT && outputTreePane.isSearchActive()) {
				outputTreePane.clearSearch();
			}
			if (focus == Focus.QUERY) {
				focus = Focus.DIAGNOSTICS;
			} else if (focus == Focus.DIAGNOSTICS) {
				focus = Focus.INPUT;
			} else if (focus == Focus.INPUT) {
				focus = Focus.OUTPUT;
			} else {
				focus = Focus.QUERY;
			}
			return true;
		}

		// PageUp / PageDown scrolls the active preview
		if (key.isPageUp()) {
			if (focus == Focus.INPUT) {
				return inputTreePane.handleKey(key);
			} else if (focus == Focus.OUTPUT) {
				return outputTreePane.handleKey(key);
			} else if (focus == Focus.DIAGNOSTICS) {
				diagnosticsScrollOffset = Math.max(0, diagnosticsScrollOffset - PAGE_SCROLL);
				return true;
			}
		}
		if (key.isPageDown()) {
			if (focus == Focus.INPUT) {
				return inputTreePane.handleKey(key);
			} else if (focus == Focus.OUTPUT) {
				return outputTreePane.handleKey(key);
			} else if (focus == Focus.DIAGNOSTICS) {
				int maxScroll = Math.max(0, diagnosticLines.size() - diagnosticsViewportHeight);
				diagnosticsScrollOffset = Math.min(maxScroll, diagnosticsScrollOffset + PAGE_SCROLL);
				return true;
			}
		}

		// Diagnostics focus navigation
		if (focus == Focus.DIAGNOSTICS) {
			if (key.isConfirm() || key.code() == KeyCode.ENTER) {
				return true;
			}
			if (key.isUp()) {
				diagnosticsScrollOffset = Math.max(0, diagnosticsScrollOffset - 1);
				return true;
			}
			if (key.isDown()) {
				int maxScroll = Math.max(0, diagnosticLines.size() - diagnosticsViewportHeight);
				diagnosticsScrollOffset = Math.min(maxScroll, diagnosticsScrollOffset + 1);
				return true;
			}
			if (key.isHome()) {
				diagnosticsScrollOffset = 0;
				return true;
			}
			if (key.isEnd()) {
				diagnosticsScrollOffset = Math.max(0, diagnosticLines.size() - diagnosticsViewportHeight);
				return true;
			}
			return false;
		}

		// Input focus navigation
		if (focus == Focus.INPUT) {
			if (inputTreePane.handleKey(key)) {
				inputScrollOffset = inputTreePane.viewMode() == JsonTreePane.ViewMode.TREE
						? inputTreePane.treeState().offset()
						: inputTreePane.textScrollOffset();
				return true;
			}
			if (key.isConfirm() || key.code() == KeyCode.ENTER) {
				return true;
			}
			return false;
		}

		// Output focus navigation
		if (focus == Focus.OUTPUT) {
			if (outputTreePane.handleKey(key)) {
				outputScrollOffset = outputTreePane.viewMode() == JsonTreePane.ViewMode.TREE
						? outputTreePane.treeState().offset()
						: outputTreePane.textScrollOffset();
				return true;
			}
			if (key.isConfirm() || key.code() == KeyCode.ENTER) {
				return true;
			}
			return false;
		}

		// Query focus navigation & editing
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			queryState.insert('\n');
			updateEvaluation();
			return true;
		}
		if (key.isUp()) {
			queryState.moveCursorUp();
			return true;
		}
		if (key.isDown()) {
			queryState.moveCursorDown();
			return true;
		}
		if (key.isLeft()) {
			queryState.moveCursorLeft();
			return true;
		}
		if (key.isRight()) {
			queryState.moveCursorRight();
			return true;
		}
		if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
			queryState.moveCursorToLineStart();
			return true;
		}
		if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
			queryState.moveCursorToLineEnd();
			return true;
		}
		if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE) {
			queryState.deleteBackward();
			updateEvaluation();
			return true;
		}
		if (key.isDeleteForward() || key.code() == KeyCode.DELETE) {
			queryState.deleteForward();
			updateEvaluation();
			return true;
		}
		if (key.hasCtrl() && key.isChar('u')) {
			while (queryState.cursorCol() > 0) {
				queryState.deleteBackward();
			}
			updateEvaluation();
			return true;
		}
		if (key.hasCtrl() && key.isChar('k')) {
			String line = queryState.getLine(queryState.cursorRow());
			int remaining = line.length() - queryState.cursorCol();
			for (int i = 0; i < remaining; i++) {
				queryState.deleteForward();
			}
			updateEvaluation();
			return true;
		}
		if (key.hasCtrl() && key.isChar('w')) {
			String line = queryState.getLine(queryState.cursorRow());
			int col = queryState.cursorCol();
			if (col > 0) {
				@Var int i = col - 1;
				while (i > 0 && Character.isWhitespace(line.charAt(i))) {
					queryState.deleteBackward();
					i--;
				}
				while (i >= 0 && !Character.isWhitespace(line.charAt(i))) {
					queryState.deleteBackward();
					i--;
				}
				updateEvaluation();
			}
			return true;
		}
		if (!key.hasCtrl() && !key.hasAlt()) {
			String str = key.string();
			if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || str.charAt(0) >= 32)) {
				queryState.insert(str);
				updateEvaluation();
				return true;
			}
		}
		return false;
	}

	private void render(dev.tamboui.terminal.Frame frame) {
		Rect area = frame.area();
		if (area.height() < 10) {
			frame.renderWidget(Paragraph.from("Screen too small for playground"), area);
			return;
		}

		int queryLines = queryState.lineCount();
		int maxQueryHeight = Math.max(4, Math.min(10, area.height() * 40 / 100));
		int queryHeight = Math.min(queryLines + 3, maxQueryHeight);

		int desiredContentRows = Math.max(1, Math.min(5, diagnosticLines.size()));
		int desiredDiagHeight = desiredContentRows + 2;
		int minBottomHeight = 3;
		int maxDiagHeight = Math.max(3, area.height() - queryHeight - minBottomHeight);
		int diagnosticsHeight = Math.min(desiredDiagHeight, maxDiagHeight);

		boolean showGuide = area.height() - queryHeight - diagnosticsHeight - minBottomHeight >= 3;
		List<Constraint> constraints = new ArrayList<>();
		constraints.add(Constraint.length(queryHeight));
		constraints.add(Constraint.length(diagnosticsHeight));
		constraints.add(Constraint.fill());
		if (showGuide) {
			constraints.add(Constraint.length(3));
		}
		List<Rect> chunks = Layout.vertical()
				.constraints(constraints)
				.split(area);

		// Top: Query pane
		Rect queryRect = chunks.get(0);
		Block queryBlock = Block.builder()
				.title(" Query ")
				.borders(Borders.ALL)
				.borderColor(focus == Focus.QUERY ? Color.CYAN : Color.DARK_GRAY)
				.build();
		frame.renderWidget(queryBlock, queryRect);
		Rect queryInnerRect = queryBlock.inner(queryRect);
		Rect queryEditorRect = new Rect(
				queryInnerRect.x(), queryInnerRect.y(), queryInnerRect.width(), Math.max(0, queryInnerRect.height() - 1));
		Rect queryStatusRect = new Rect(
				queryInnerRect.x(), queryInnerRect.bottom() - 1, queryInnerRect.width(), 1);
		TextArea textArea = TextArea.builder()
				.showLineNumbers(true)
				.build();
		if (focus == Focus.QUERY && modal == Modal.NONE) {
			textArea.renderWithCursor(queryEditorRect, frame.buffer(), queryState, frame);
		} else {
			textArea.render(queryEditorRect, frame.buffer(), queryState);
		}
		Line autoRunStatusLine = buildAutoRunStatusLine();
		int autoRunStatusWidth = autoRunStatusLine.width();
		List<Rect> statusChunks = Layout.horizontal()
				.constraints(Constraint.fill(), Constraint.length(autoRunStatusWidth))
				.split(queryStatusRect);
		Rect leftStatusRect = statusChunks.get(0);
		Rect rightStatusRect = statusChunks.get(1);

		frame.renderWidget(Paragraph.builder()
				.text(Text.from(autoRunStatusLine))
				.alignment(Alignment.RIGHT)
				.build(), rightStatusRect);

		if (leftStatusRect.width() > 0) {
			if (evaluationStatus == EvaluationStatus.LOADING) {
				frame.renderWidget(Paragraph.builder()
						.text(Text.from(Line.styled("  Evaluating...", Style.EMPTY.cyan())))
						.build(), leftStatusRect);
				if (leftStatusRect.width() > 1) {
					frame.renderStatefulWidget(
							Spinner.builder().spinnerStyle(SpinnerStyle.DOTS).style(Style.EMPTY.cyan()).build(),
							new Rect(leftStatusRect.x() + 1, leftStatusRect.y(), 1, 1),
							spinnerState);
				}
			} else if (evaluationStatus == EvaluationStatus.FAILURE) {
				frame.renderWidget(Paragraph.builder()
						.text(Text.from(Line.styled(" ✗ Error ", Style.EMPTY.red())))
						.build(), leftStatusRect);
			} else if (evaluationStatus == EvaluationStatus.STALE) {
				frame.renderWidget(Paragraph.builder()
						.text(Text.from(Line.styled(" ○ Pending Run ", Style.EMPTY.dim())))
						.build(), leftStatusRect);
			} else {
				frame.renderWidget(Paragraph.builder()
						.text(Text.from(Line.styled(" ✓ Up to date ", Style.EMPTY.green())))
						.build(), leftStatusRect);
			}
		}
		frame.clearCursor();

		// Middle: Diagnostics pane
		Rect diagnosticsRect = chunks.get(1);
		Block diagnosticsBlock = Block.builder()
				.title(" Diagnostics ")
				.borders(Borders.ALL)
				.borderColor(focus == Focus.DIAGNOSTICS ? Color.CYAN : Color.DARK_GRAY)
				.build();
		Paragraph diagParagraph = Paragraph.builder()
				.block(diagnosticsBlock)
				.text(Text.from(diagnosticLines))
				.scroll(diagnosticsScrollOffset)
				.build();
		frame.renderWidget(diagParagraph, diagnosticsRect);

		int diagInnerHeight = diagnosticsBlock.inner(diagnosticsRect).height();
		this.diagnosticsViewportHeight = Math.max(1, diagInnerHeight);
		if (diagnosticLines.size() > diagInnerHeight && diagInnerHeight > 0) {
			Scrollbar diagScrollbar = Scrollbar.builder()
					.orientation(ScrollbarOrientation.VERTICAL_RIGHT)
					.trackStyle(Style.EMPTY.fg(focus == Focus.DIAGNOSTICS ? Color.CYAN : Color.DARK_GRAY))
					.thumbStyle(Style.EMPTY.fg(focus == Focus.DIAGNOSTICS ? Color.WHITE : Color.GRAY))
					.build();
			ScrollbarState diagState = new ScrollbarState()
					.contentLength(diagnosticLines.size())
					.viewportContentLength(diagInnerHeight)
					.position(diagnosticsScrollOffset);
			Rect scrollbarRect = new Rect(diagnosticsRect.x(), diagnosticsRect.y() + 1, diagnosticsRect.width(), diagInnerHeight);
			frame.renderStatefulWidget(diagScrollbar, scrollbarRect, diagState);
		}

		// Bottom: Side-by-side Input and Output preview panes
		Rect bottomRect = chunks.get(2);
		List<Rect> bottomPanes = Layout.horizontal()
				.constraints(Constraint.percentage(50), Constraint.percentage(50))
				.split(bottomRect);
		Rect inputRect = bottomPanes.get(0);
		Rect outputRect = bottomPanes.get(1);

		// Left: Input view pane
		String inMode = inputTreePane.viewMode() == JsonTreePane.ViewMode.TREE ? "Tree" : "Text";
		String inStats = String.format("%d %s (%d %s)",
				inputs.size(), inputs.size() == 1 ? "item" : "items",
				inputLines.size(), inputLines.size() == 1 ? "line" : "lines");
		String inputTitle = String.format(" Input [%s]: %s ", inMode, inStats);
		Block inputBlock = Block.builder()
				.title(inputTitle)
				.borders(Borders.ALL)
				.borderColor(focus == Focus.INPUT && modal == Modal.NONE ? Color.CYAN : Color.DARK_GRAY)
				.build();
		String inputEmptyMessage = inputErrorMessage != null
				? "(error parsing input: " + inputErrorMessage + ")"
				: (inputs.isEmpty() ? "(no input)" : null);
		inputTreePane.render(inputRect, frame.buffer(), frame, focus == Focus.INPUT && modal == Modal.NONE, inputBlock, inputEmptyMessage);
		this.inputViewportHeight = Math.max(1, inputBlock.inner(inputRect).height());
		this.inputScrollOffset = inputTreePane.viewMode() == JsonTreePane.ViewMode.TREE
				? inputTreePane.treeState().offset()
				: inputTreePane.textScrollOffset();

		// Right: Output preview pane
		String outMode = outputTreePane.viewMode() == JsonTreePane.ViewMode.TREE ? "Tree" : "Text";
		String outStats = String.format("%d %s (%d %s)",
				itemCount, itemCount == 1 ? "item" : "items",
				previewLines.size(), previewLines.size() == 1 ? "line" : "lines");
		String outputTitle = String.format(" Output Preview [%s]%s: %s ", outMode, outputStale ? " (Stale)" : "", outStats);
		Block previewBlock = Block.builder()
				.title(outputTitle)
				.borders(Borders.ALL)
				.borderColor(focus == Focus.OUTPUT && modal == Modal.NONE ? Color.CYAN : Color.DARK_GRAY)
				.build();
		String outputEmptyMessage = errorMessage != null
				? errorMessage
				: (itemCount == 0 ? "(no output)" : null);
		outputTreePane.render(outputRect, frame.buffer(), frame, focus == Focus.OUTPUT && modal == Modal.NONE, previewBlock, outputEmptyMessage);
		this.outputViewportHeight = Math.max(1, previewBlock.inner(outputRect).height());
		this.outputScrollOffset = outputTreePane.viewMode() == JsonTreePane.ViewMode.TREE
				? outputTreePane.treeState().offset()
				: outputTreePane.textScrollOffset();

		// Bottom: Keyboard navigation guide
		if (showGuide) {
			Rect guideRect = chunks.get(3);
			Block guideBlock = Block.builder()
					.borders(Borders.ALL)
					.borderType(BorderType.ROUNDED)
					.borderColor(Color.DARK_GRAY)
					.build();
			Line guideLine = buildGuideLine(focus);
			Paragraph guideParagraph = Paragraph.builder()
					.block(guideBlock)
					.text(Text.from(guideLine))
					.build();
			frame.renderWidget(guideParagraph, guideRect);
		}

		// Modal confirmation dialog overlay
		if (modal == Modal.OPTIONS) {
			int dialogWidth = Math.min(78, Math.max(50, area.width() - 4));
			int dialogHeight = 20;
			int dialogX = area.left() + (area.width() - dialogWidth) / 2;
			int dialogY = area.top() + (area.height() - dialogHeight) / 2;
			Rect dialogArea = new Rect(dialogX, dialogY, dialogWidth, dialogHeight);

			frame.renderWidget(Clear.INSTANCE, dialogArea);

			Block dialogBlock = Block.builder()
					.title(" Options ")
					.borders(Borders.ALL)
					.borderColor(Color.CYAN)
					.build();

			List<Line> optionLines = new ArrayList<>();
			optionLines.add(Line.styled(" Input:", Style.EMPTY.bold()));
			optionLines.add(buildOptionLine(0, rawInput, "-R, --raw-input", "read each line as string"));
			optionLines.add(buildOptionLine(1, slurp, "-s, --slurp", "read all inputs into an array"));
			optionLines.add(Line.styled(" Output:", Style.EMPTY.bold()));
			optionLines.add(buildOptionLine(2, compact, "-c, --compact", "compact JSON output"));
			optionLines.add(buildOptionLine(3, rawOutput, "-r, --raw-output", "output raw strings"));
			optionLines.add(Line.styled(" Engine:", Style.EMPTY.bold()));
			optionLines.add(buildSelectorLine(4, "Version:", "< " + version + " >"));
			optionLines.add(buildSelectorLine(5, "Provider:", "< " + providerName + " >"));
			optionLines.add(Line.styled(" Runtime Limits:", Style.EMPTY.bold()));
			optionLines.add(buildRuntimeLimitLine(MAX_STRING_LENGTH_OPTION, "--max-string-length", "string length"));
			optionLines.add(buildRuntimeLimitLine(MAX_BINARY_LENGTH_OPTION, "--max-binary-length", "binary bytes"));
			optionLines.add(buildRuntimeLimitLine(MAX_ARRAY_LENGTH_OPTION, "--max-array-length", "array elements"));
			optionLines.add(buildRuntimeLimitLine(MAX_OBJECT_MEMBER_COUNT_OPTION, "--max-object-member-count", "object members"));
			optionLines.add(buildRuntimeLimitLine(MAX_USER_DEFINED_FUNCTION_CALLS_OPTION, "--max-user-defined-function-calls", "query calls"));
			optionLines.add(buildRuntimeLimitLine(MAX_OUTPUTS_PER_EXPRESSION_OPTION, "--max-outputs-per-expression", "expression outputs"));
			optionLines.add(Line.from(Span.raw("")));
			optionLines.add(Line.from(
					Span.styled("  [↑↓] Select  [0-9/⌫] Edit  [Space/←→] Change  [Esc/Enter] Close", Style.EMPTY.dim().yellow())
			));

			Paragraph dialogContent = Paragraph.builder()
					.block(dialogBlock)
					.text(Text.from(optionLines))
					.build();
			frame.renderWidget(dialogContent, dialogArea);
			if (isRuntimeLimitOption(selectedOptionIndex)) {
				String value = formatRuntimeLimit(selectedOptionIndex);
				int row = 10 + selectedOptionIndex - FIRST_RUNTIME_LIMIT_OPTION;
				int cursorX = dialogArea.left() + 1 + 4 + LIMIT_FLAG_WIDTH + value.length();
				int cursorY = dialogArea.top() + 1 + row;
				frame.setCursorPosition(new Position(cursorX, cursorY));
			}
		} else if (modal != Modal.NONE) {
			int dialogWidth = Math.min(64, Math.max(36, area.width() - 4));
			int dialogHeight = 7;
			int dialogX = area.left() + (area.width() - dialogWidth) / 2;
			int dialogY = area.top() + (area.height() - dialogHeight) / 2;
			Rect dialogArea = new Rect(dialogX, dialogY, dialogWidth, dialogHeight);

			frame.renderWidget(Clear.INSTANCE, dialogArea);

			String title = modal == Modal.CONFIRM_QUIT ? " Quit Playground " : " Apply & Exit ";
			String question = modal == Modal.CONFIRM_QUIT
					? "Discard changes and exit without emitting results?"
					: "Apply query and output results to stdout?";
			String options = modal == Modal.CONFIRM_QUIT
					? "[y] Yes, quit    [n] No, keep editing"
					: "[y/Enter] Yes, apply    [n] No, keep editing";

			Block dialogBlock = Block.builder()
					.title(title)
					.borders(Borders.ALL)
					.borderColor(modal == Modal.CONFIRM_QUIT ? Color.RED : Color.GREEN)
					.build();

			Paragraph dialogContent = Paragraph.builder()
					.block(dialogBlock)
					.text(Text.from(
							Line.from(Span.raw("")),
							Line.from(Span.styled(question, Style.EMPTY.bold())),
							Line.from(Span.raw("")),
							Line.from(Span.styled(options, Style.EMPTY.dim().yellow()))
					))
					.alignment(Alignment.CENTER)
					.build();
			frame.renderWidget(dialogContent, dialogArea);
		}
	}

	private Line buildOptionLine(int index, boolean checked, String flag, String desc) {
		boolean isSelected = (selectedOptionIndex == index);
		String pointer = isSelected ? "> " : "  ";
		String check = checked ? "[x] " : "[ ] ";

		Style prefixStyle = isSelected ? Style.EMPTY.bold().cyan() : Style.EMPTY;
		Style flagStyle = isSelected ? Style.EMPTY.bold().white() : Style.EMPTY.bold();
		Style descStyle = isSelected ? Style.EMPTY.cyan() : Style.EMPTY.dim();

		return Line.from(
				Span.styled("  " + pointer, prefixStyle),
				Span.styled(check, checked ? Style.EMPTY.bold().green() : Style.EMPTY.dim()),
				Span.styled(String.format("%-18s", flag), flagStyle),
				Span.styled(desc, descStyle)
		);
	}

	private Line buildSelectorLine(int index, String label, String value) {
		boolean isSelected = (selectedOptionIndex == index);
		String pointer = isSelected ? "> " : "  ";

		Style prefixStyle = isSelected ? Style.EMPTY.bold().cyan() : Style.EMPTY;
		Style labelStyle = isSelected ? Style.EMPTY.bold().white() : Style.EMPTY.bold();
		Style valueStyle = isSelected ? Style.EMPTY.bold().cyan() : Style.EMPTY.bold().green();

		return Line.from(
				Span.styled("  " + pointer, prefixStyle),
				Span.styled(String.format("  %-20s", label), labelStyle),
				Span.styled(value, valueStyle)
		);
	}

	private Line buildRuntimeLimitLine(int index, String flag, String description) {
		boolean isSelected = selectedOptionIndex == index;
		String value = formatRuntimeLimit(index);
		int paddingWidth = LIMIT_VALUE_WIDTH - value.length() - (isSelected ? 1 : 0);
		String padding = paddingWidth > 0 ? String.format("%" + paddingWidth + "s", "") : "";

		Style prefixStyle = isSelected ? Style.EMPTY.bold().cyan() : Style.EMPTY;
		Style flagStyle = isSelected ? Style.EMPTY.bold().white() : Style.EMPTY.bold();
		Style valueStyle = isSelected ? Style.EMPTY.bold().cyan() : Style.EMPTY.bold().green();
		Style descriptionStyle = isSelected ? Style.EMPTY.cyan() : Style.EMPTY.dim();

		return Line.from(
				Span.styled(isSelected ? "  > " : "    ", prefixStyle),
				Span.styled(String.format("%-" + LIMIT_FLAG_WIDTH + "s", flag), flagStyle),
				Span.styled(value, valueStyle),
				Span.styled(isSelected ? " " : "", Style.EMPTY.reversed()),
				Span.raw(padding),
				Span.styled(description, descriptionStyle)
		);
	}

	private String formatRuntimeLimit(int index) {
		long value = getRuntimeLimit(index);
		return value == getRuntimeLimitMaximum(index) ? "unlimited" : Long.toString(value);
	}

	private void updateEvaluation() {
		updateEvaluationGeneric(jsonProvider, env, inputs, false);
	}

	private void evaluateNow() {
		updateEvaluationGeneric(jsonProvider, env, inputs, true);
	}

	private static final class EvaluationResult {
		final @Nullable List<String> previewLines;
		final @Nullable List<?> outputItems;
		final int itemCount;
		final @Nullable String errorMessage;
		final List<Diagnostic> warnings;

		EvaluationResult(
				@Nullable List<String> previewLines,
				@Nullable List<?> outputItems,
				int itemCount,
				@Nullable String errorMessage,
				List<Diagnostic> warnings) {
			this.previewLines = previewLines;
			this.outputItems = outputItems;
			this.itemCount = itemCount;
			this.errorMessage = errorMessage;
			this.warnings = warnings;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> EvaluationResult computeEvaluation(
			JsonProvider<T> provider,
			Environment<?> environment,
			List<?> inList,
			String queryText,
			CompileOptions compileOptions,
			RuntimeOptions runtimeOptions,
			boolean warningsEnabled,
			boolean rawOutput,
			boolean compact,
			boolean execute) {
		List<Diagnostic> currentWarnings = new ArrayList<>();
		try {
			CompileOptions.Builder optsBuilder = CompileOptions.newBuilder()
					.setOptimizationOptions(compileOptions.getOptimizationOptions());
			if (warningsEnabled) {
				optsBuilder.setDiagnosticListener(diag -> {
					if (diag.severity() == Diagnostic.Severity.WARNING) {
						currentWarnings.add(diag);
					}
				});
			}
			JsonQuery<T> jq = ((Environment<T>) environment).compile(queryText, optsBuilder.build()).withRuntimeOptions(runtimeOptions);
			if (!execute) {
				return new EvaluationResult(null, null, 0, null, currentWarnings);
			}
			List<String> lines = new ArrayList<>();
			List<Object> items = new ArrayList<>();
			int[] count = new int[1];
			for (Object tree : inList) {
				jq.apply((T) tree, output -> {
					count[0]++;
					items.add(output);
					String formatted;
					if (provider.isString(output) && rawOutput) {
						formatted = provider.getString(output);
					} else if (compact) {
						formatted = provider.format(output);
					} else {
						formatted = JqPrettyPrinter.print(provider, output, PRETTY_INDENT);
					}
					for (String line : formatted.split("\r?\n", -1)) {
						lines.add(line);
					}
				});
			}
			return new EvaluationResult(lines, items, count[0], null, currentWarnings);
		} catch (Throwable t) {
			String err = t.getMessage() != null ? t.getMessage() : t.toString();
			return new EvaluationResult(null, null, 0, err, currentWarnings);
		}
	}

	private void applyEvaluationResult(EvaluationResult result) {
		this.warnings = result.warnings;
		if (result.errorMessage != null) {
			this.errorMessage = result.errorMessage;
			this.outputStale = true;
			this.evaluationStatus = EvaluationStatus.FAILURE;
		} else if (result.previewLines == null) {
			this.errorMessage = null;
			this.outputStale = true;
			this.evaluationStatus = EvaluationStatus.STALE;
		} else {
			this.previewLines = result.previewLines;
			this.itemCount = result.itemCount;
			this.errorMessage = null;
			this.outputStale = false;
			this.evaluationStatus = EvaluationStatus.UP_TO_DATE;
			this.outputScrollOffset = 0;
			int maxOutputScroll = Math.max(0, result.previewLines.size() - outputViewportHeight);
			if (outputScrollOffset > maxOutputScroll) {
				outputScrollOffset = maxOutputScroll;
			}
			this.outputTreePane.setNodes(result.outputItems != null ? result.outputItems : Collections.emptyList(), this.previewLines, jsonProvider);
		}
		updateDiagnosticLines();
	}

	// Safe unchecked cast: environment and inputs are always created with the matching jsonProvider.
	@SuppressWarnings("unchecked")
	private <T> void updateEvaluationGeneric(
			JsonProvider<T> provider, Environment<?> environment, List<?> inList, boolean applyWhilePaused) {
		if (inputErrorMessage != null) {
			this.errorMessage = "Input error: " + inputErrorMessage;
			this.outputStale = true;
			this.evaluationStatus = EvaluationStatus.FAILURE;
			updateDiagnosticLines();
			return;
		}

		boolean execute = !automaticEvaluationPaused || applyWhilePaused;
		String queryText = queryState.text();
		CompileOptions currentCompileOptions = compileOptions;
		RuntimeOptions currentRuntimeOptions = runtimeOptions;
		boolean currentWarningsEnabled = warningsEnabled;
		boolean currentRawOutput = rawOutput;
		boolean currentCompact = compact;

		Executor executor = this.evaluationExecutor;
		TuiRunner r = this.runner;
		if (executor != null && r != null) {
			this.evaluationStatus = EvaluationStatus.LOADING;
			this.spinnerState.reset();
			long currentVersion = ++this.evaluationVersion;
			executor.execute(() -> {
				EvaluationResult res = computeEvaluation(
						provider, environment, inList, queryText,
						currentCompileOptions, currentRuntimeOptions,
						currentWarningsEnabled, currentRawOutput, currentCompact,
						execute);
				if (r.isRunning()) {
					r.runOnRenderThread(() -> {
						if (currentVersion == this.evaluationVersion) {
							applyEvaluationResult(res);
							redrawRequested = false;
							r.draw(this::render);
						}
					});
				} else {
					synchronized (Playground.this) {
						if (currentVersion == this.evaluationVersion) {
							applyEvaluationResult(res);
						}
					}
				}
			});
		} else {
			applyEvaluationResult(computeEvaluation(
					provider, environment, inList, queryText,
					currentCompileOptions, currentRuntimeOptions,
					currentWarningsEnabled, currentRawOutput, currentCompact,
					execute));
		}
	}

	private void updateDiagnosticLines() {
		List<Line> dLines = new ArrayList<>();
		List<String> plain = new ArrayList<>();
		if (errorMessage == null && warnings.isEmpty()) {
			dLines.add(Line.from(Span.styled("(no diagnostics)", Style.EMPTY.dim())));
			plain.add("(no diagnostics)");
		} else {
			if (errorMessage != null) {
				@Var String err = errorMessage;
				while (err.endsWith("\n") || err.endsWith("\r")) {
					err = err.substring(0, err.length() - 1);
				}
				String[] split = err.split("\r?\n", -1);
				for (int i = 0; i < split.length; i++) {
					String line = split[i];
					if (i == 0) {
						dLines.add(Line.from(Span.styled("[Error] " + line, Style.EMPTY.bold().red())));
						plain.add("[Error] " + line);
					} else {
						dLines.add(Line.from(Span.styled(line, Style.EMPTY.bold().red())));
						plain.add(line);
					}
				}
			}
			for (Diagnostic w : warnings) {
				@Var String msg = w.message() + (w.location() != null ? " at " + w.location() : "");
				while (msg.endsWith("\n") || msg.endsWith("\r")) {
					msg = msg.substring(0, msg.length() - 1);
				}
				String[] split = msg.split("\r?\n", -1);
				for (int i = 0; i < split.length; i++) {
					String line = split[i];
					if (i == 0) {
						dLines.add(Line.from(Span.styled("[Warning] " + line, Style.EMPTY.bold().yellow())));
						plain.add("[Warning] " + line);
					} else {
						dLines.add(Line.from(Span.styled(line, Style.EMPTY.bold().yellow())));
						plain.add(line);
					}
				}
			}
		}
		this.diagnosticLines = Collections.unmodifiableList(dLines);
		this.diagnosticPlainLines = Collections.unmodifiableList(plain);
		this.diagnosticsScrollOffset = 0;
	}

	private void emitResults() throws Exception {
		emitResultsGeneric(jsonProvider, env, inputs);
	}

	String finalCommand() {
		StringBuilder command = new StringBuilder("jackson-jq");
		if (compact) {
			command.append(" -c");
		}
		if (rawOutput) {
			command.append(" -r");
		}
		if (nullInput) {
			command.append(" -n");
		}
		if (rawInput) {
			command.append(" -R");
		}
		if (slurp) {
			command.append(" -s");
		}
		if (!version.equals(Versions.JQ_1_6)) {
			command.append(" --jq ").append(version);
		}
		if (!providerName.equals("jackson3")) {
			command.append(" --json-provider ").append(providerName);
		}
		if (!warningsEnabled) {
			command.append(" --no-warnings");
		}
		if (!compileOptions.getOptimizationOptions().getTailCallOptimization()) {
			command.append(" --disable-tco");
		}
		appendRuntimeLimit(command, "--max-string-length", runtimeOptions.getMaxStringLength(), Integer.MAX_VALUE);
		appendRuntimeLimit(command, "--max-binary-length", runtimeOptions.getMaxBinaryLength(), Integer.MAX_VALUE);
		appendRuntimeLimit(command, "--max-array-length", runtimeOptions.getMaxArrayLength(), Integer.MAX_VALUE);
		appendRuntimeLimit(command, "--max-object-member-count", runtimeOptions.getMaxObjectMemberCount(), Integer.MAX_VALUE);
		appendRuntimeLimit(command, "--max-user-defined-function-calls", runtimeOptions.getMaxUserDefinedFunctionCalls(), Long.MAX_VALUE);
		appendRuntimeLimit(command, "--max-outputs-per-expression", runtimeOptions.getMaxOutputsPerExpression(), Long.MAX_VALUE);
		command.append(" -- ").append(shellQuote(queryState.text()));
		for (String inputFile : inputFiles) {
			command.append(' ').append(shellQuote(inputFile));
		}
		return command.toString();
	}

	private static void appendRuntimeLimit(StringBuilder command, String option, long value, long unlimited) {
		if (value != unlimited) {
			command.append(' ').append(option).append(' ').append(value);
		}
	}

	private static String shellQuote(String value) {
		return "'" + value.replace("'", "'\"'\"'") + "'";
	}

	// Safe unchecked cast: environment and inputs are always created with the matching jsonProvider.
	@SuppressWarnings("unchecked")
	private <T> void emitResultsGeneric(JsonProvider<T> provider, Environment<?> environment, List<?> inList) throws Exception {
		try {
			JsonQuery<T> jq = ((Environment<T>) environment).compile(queryState.text(), compileOptions).withRuntimeOptions(runtimeOptions);
			for (Object tree : inList) {
				jq.apply((T) tree, output -> {
					if (provider.isString(output) && rawOutput) {
						out.println(provider.getString(output));
					} else if (compact) {
						out.println(provider.format(output));
					} else {
						out.println(JqPrettyPrinter.print(provider, output, PRETTY_INDENT));
					}
				});
			}
			err.println(finalCommand());
		} catch (Exception e) {
			err.println("jq: error: " + e.getMessage());
		}
	}

	String getQuery() {
		return queryState.text();
	}

	boolean isAccepted() {
		return accepted;
	}

	Focus getFocus() {
		return focus;
	}

	Modal getModal() {
		return modal;
	}

	@Nullable
	String getErrorMessage() {
		return errorMessage;
	}

	List<String> getInputLines() {
		return inputLines;
	}

	int getInputScrollOffset() {
		return inputScrollOffset;
	}

	int getOutputScrollOffset() {
		return outputScrollOffset;
	}

	int getInputSelectedIndex() {
		return inputTreePane.treeState().selected();
	}

	int getOutputSelectedIndex() {
		return outputTreePane.selectedIndex();
	}

	JsonTreePane getInputTreePane() {
		return inputTreePane;
	}

	JsonTreePane getOutputTreePane() {
		return outputTreePane;
	}

	List<String> getPreviewLines() {
		return previewLines;
	}

	int getInputViewportHeight() {
		return inputViewportHeight;
	}

	int getOutputViewportHeight() {
		return outputViewportHeight;
	}

	int getDiagnosticsViewportHeight() {
		return diagnosticsViewportHeight;
	}

	int getDiagnosticsScrollOffset() {
		return diagnosticsScrollOffset;
	}

	List<String> getDiagnosticPlainLines() {
		return Collections.unmodifiableList(diagnosticPlainLines);
	}

	List<Diagnostic> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	boolean isRawInput() {
		return rawInput;
	}

	boolean isSlurp() {
		return slurp;
	}

	boolean isCompact() {
		return compact;
	}

	boolean isRawOutput() {
		return rawOutput;
	}

	Version getVersion() {
		return version;
	}

	String getProviderName() {
		return providerName;
	}

	JsonProvider<?> getJsonProvider() {
		return jsonProvider;
	}

	RuntimeOptions getRuntimeOptions() {
		return runtimeOptions;
	}

	int getSelectedOptionIndex() {
		return selectedOptionIndex;
	}

	boolean isAutomaticEvaluationPaused() {
		return automaticEvaluationPaused;
	}

	boolean isOutputStale() {
		return outputStale;
	}

	EvaluationStatus getEvaluationStatus() {
		return evaluationStatus;
	}

	SpinnerState getSpinnerState() {
		return spinnerState;
	}

	void setEvaluationExecutor(@Nullable Executor evaluationExecutor) {
		this.evaluationExecutor = evaluationExecutor;
	}

	Line buildAutoRunStatusLine() {
		String status = automaticEvaluationPaused ? " Auto-run: Paused" : " Auto-run: On";
		Style statusStyle = automaticEvaluationPaused ? Style.EMPTY.bold().yellow() : Style.EMPTY.green();
		return Line.from(
				Span.styled(status, statusStyle),
				Span.styled(" (Ctrl+P to toggle) ", Style.EMPTY.dim()));
	}

	Line buildGuideLine(Focus focus) {
		List<Span> spans = new ArrayList<>();
		switch (focus) {
			case QUERY:
				addGuideItem(spans, "Enter", "Newline");
				addGuideItem(spans, "Tab", "Focus Next");
				addGuideItem(spans, "Ctrl+R", "Run Query");
				addGuideItem(spans, "Ctrl+O", "Options");
				addGuideItem(spans, "Esc", "Emit & Quit");
				addGuideItem(spans, "Ctrl+C", "Quit");
				break;
			case DIAGNOSTICS:
				addGuideItem(spans, "↑↓", "Scroll");
				addGuideItem(spans, "Tab", "Focus Next");
				addGuideItem(spans, "Ctrl+R", "Run Query");
				addGuideItem(spans, "Ctrl+O", "Options");
				addGuideItem(spans, "Esc", "Emit & Quit");
				addGuideItem(spans, "Ctrl+C", "Quit");
				break;
			case INPUT:
			case OUTPUT:
				JsonTreePane pane = (focus == Focus.INPUT) ? inputTreePane : outputTreePane;
				addGuideItem(spans, "↑↓/jk", "Navigate");
				addGuideItem(spans, "Ctrl+U/Ctrl+D", "Page Up/Down");
				if (pane.viewMode() == JsonTreePane.ViewMode.TREE) {
					addGuideItem(spans, "←→/hl", "Collapse/Expand");
					addGuideItem(spans, "Space", "Toggle");
				}
				addGuideItem(spans, "t", "Toggle View");
				addGuideItem(spans, "/", "Search");
				addGuideItem(spans, "Tab", "Focus Next");
				addGuideItem(spans, "Ctrl+R", "Run Query");
				addGuideItem(spans, "Ctrl+O", "Options");
				// While searching, Esc is consumed by the pane (it clears the search) and the search
				// bar shows its own (n/p/Esc) hint.
				if (!pane.isSearchActive() && pane.searchQuery().isEmpty()) {
					addGuideItem(spans, "Esc", "Emit & Quit");
				}
				addGuideItem(spans, "Ctrl+C", "Quit");
				break;
		}
		return Line.from(spans);
	}

	private static void addGuideItem(List<Span> spans, String key, String action) {
		if (!spans.isEmpty()) {
			spans.add(Span.raw("   "));
		}
		spans.add(Span.styled(key, Style.EMPTY.bold().cyan()));
		spans.add(Span.raw(" " + action));
	}
}
