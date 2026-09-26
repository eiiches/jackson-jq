package net.thisptr.jackson.jq.v2.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.errorprone.annotations.Var;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextAreaState;
import org.jspecify.annotations.Nullable;

final class VimQueryEditor {
	enum Mode {
		NORMAL,
		INSERT,
		REPLACE,
		COMMAND,
		SEARCH,
		VISUAL,
		VISUAL_LINE,
		VISUAL_BLOCK
	}

	record Result(boolean handled, boolean textChanged, boolean submitRequested) {
		private static final Result HANDLED = new Result(true, false, false);
		private static final Result CHANGED = new Result(true, true, false);
		private static final Result SUBMIT = new Result(true, false, true);
		private static final Result NOT_HANDLED = new Result(false, false, false);
	}

	private record Snapshot(String text, int row, int col, int preferredColumn) {
	}

	private enum RegisterType {
		CHARACTERWISE,
		LINEWISE,
		BLOCKWISE
	}

	private record Register(String text, RegisterType type, int blockWidth) {
		private static final Register EMPTY = new Register("", RegisterType.CHARACTERWISE, 0);

		Register(String text, boolean linewise) {
			this(text, linewise ? RegisterType.LINEWISE : RegisterType.CHARACTERWISE, 0);
		}

		boolean linewise() {
			return type == RegisterType.LINEWISE;
		}

		boolean blockwise() {
			return type == RegisterType.BLOCKWISE;
		}
	}

	record VisualRange(int startCol, int endCol, boolean includesNewline) {
	}

	private record BlockInsertContext(int startRow, int endRow, int col, boolean append) {
	}

	private enum Operator {
		DELETE,
		CHANGE,
		YANK
	}

	private enum TextObjectScope {
		INNER,
		AROUND
	}

	private enum Motion {
		LEFT,
		RIGHT,
		WORD_FORWARD,
		WORD_BACKWARD,
		WORD_END,
		LINE_START,
		LINE_END,
		LINE_DOWN,
		LINE_UP,
		FIRST_LINE,
		LAST_LINE,
		MATCHING_DELIMITER,
		FIND_FORWARD,
		FIND_BACKWARD,
		TILL_FORWARD,
		TILL_BACKWARD
	}

	private record MotionResult(int targetOffset, boolean linewise, boolean inclusive) {
	}

	private record CharacterSearch(Motion motion, String target) {
	}

	private record Delimiter(char value, int offset, boolean interpolation) {
	}

	private record DelimiterMap(int[] pairs, boolean[] structural, int[] doubleQuotePairs) {
	}

	private record TextObjectRange(int start, int end) {
	}

	record SearchMatch(int start, int end) {
	}

	private static final int MAX_UNDO_ENTRIES = 100;
	private static final int MAX_COUNT = 10_000;

	private final TextAreaState state;
	private final Deque<Snapshot> undoStack = new ArrayDeque<>();
	private Mode mode = Mode.NORMAL;
	private Register register = Register.EMPTY;
	private final StringBuilder command = new StringBuilder();
	private int commandCursor;
	private final StringBuilder searchInput = new StringBuilder();
	private final StringBuilder count = new StringBuilder();
	private @Nullable Operator pendingOperator;
	private @Nullable TextObjectScope pendingTextObjectScope;
	private int operatorCount = 1;
	private boolean pendingG;
	private @Nullable Motion awaitingCharacterMotion;
	private @Nullable CharacterSearch lastSearch;
	private @Nullable String searchPattern;
	private List<SearchMatch> searchMatches = List.of();
	private int activeSearchMatch = -1;
	private @Nullable Snapshot searchStart;
	private List<SearchMatch> previewSearchMatches = List.of();
	private int previewSearchMatch = -1;
	private int searchInputCursor;
	private @Nullable String searchError;
	private boolean awaitingReplace;
	private int replaceCount = 1;
	private boolean insertUndoCaptured;
	private final Deque<Snapshot> replaceHistory = new ArrayDeque<>();
	private int visualAnchorRow;
	private int visualAnchorCol;
	private @Nullable BlockInsertContext blockInsertContext;
	private final StringBuilder blockInsertedText = new StringBuilder();
	private @Nullable String message;
	private int preferredColumn;
	private @Nullable Path currentFile;
	private String savedText;

	VimQueryEditor(TextAreaState state) {
		this(state, null);
	}

	VimQueryEditor(TextAreaState state, @Nullable Path currentFile) {
		this.state = state;
		this.currentFile = currentFile;
		this.savedText = state.text();
		normalizeNormalCursor();
		this.preferredColumn = state.cursorCol();
	}

	Mode mode() {
		return mode;
	}

	boolean isVisualMode() {
		return mode == Mode.VISUAL || mode == Mode.VISUAL_LINE || mode == Mode.VISUAL_BLOCK;
	}

	String modeLabel() {
		return switch (mode) {
			case NORMAL -> "NORMAL";
			case INSERT -> "INSERT";
			case REPLACE -> "REPLACE";
			case COMMAND -> "COMMAND";
			case SEARCH -> "SEARCH";
			case VISUAL -> "VISUAL";
			case VISUAL_LINE -> "VISUAL LINE";
			case VISUAL_BLOCK -> "VISUAL BLOCK";
		};
	}

	int commandCursor() {
		return commandCursor;
	}

	String commandText() {
		return command.toString();
	}

	@Nullable
	Path currentFile() {
		return currentFile;
	}

	@Nullable
	String statusText() {
		if (mode == Mode.COMMAND) {
			return ":" + command;
		}
		if (mode == Mode.SEARCH) {
			return searchStatusText();
		}
		if (isVisualMode()) {
			return visualStatusText();
		}
		if (message != null) {
			return message;
		}
		if (searchPattern != null) {
			return searchSummary(searchPattern, searchMatches, activeSearchMatch);
		}
		return null;
	}

	private String visualStatusText() {
		if (mode == Mode.VISUAL_LINE) {
			int lines = Math.abs(state.cursorRow() - visualAnchorRow) + 1;
			return lines + (lines == 1 ? " line" : " lines");
		}
		if (mode == Mode.VISUAL_BLOCK) {
			int lines = Math.abs(state.cursorRow() - visualAnchorRow) + 1;
			int cols = Math.abs(state.cursorCol() - visualAnchorCol) + 1;
			return lines + "x" + cols;
		}
		int anchorOffset = offsetForPosition(visualAnchorRow, visualAnchorCol);
		int cursorOffset = offset();
		int lines = Math.abs(state.cursorRow() - visualAnchorRow) + 1;
		int chars = Math.abs(cursorOffset - anchorOffset) + 1;
		if (lines > 1) {
			return lines + " lines, " + chars + " characters";
		}
		return chars + (chars == 1 ? " character" : " characters");
	}

	@Nullable
	VisualRange visualRangeOnRow(int row) {
		if (!isVisualMode() || row < 0 || row >= state.lineCount()) {
			return null;
		}
		String line = state.getLine(row);
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			if (row >= minRow && row <= maxRow) {
				return new VisualRange(0, line.length(), true);
			}
			return null;
		}
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			if (row >= minRow && row <= maxRow) {
				int start = Math.min(minCol, line.length());
				int end = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
				return new VisualRange(start, end, false);
			}
			return null;
		}
		// Mode.VISUAL (characterwise)
		int[] sel = characterwiseSelectionOffsets();
		int startOffset = sel[0];
		int endOffset = sel[1];
		int lineStart = offsetForPosition(row, 0);
		int lineEnd = lineStart + line.length();
		if (lineEnd < startOffset || (lineStart >= endOffset && (startOffset < endOffset || lineStart > endOffset))) {
			return null;
		}
		int startCol = Math.max(0, startOffset - lineStart);
		int endCol = Math.min(line.length(), Math.max(startCol, endOffset - lineStart));
		boolean includesNewline = endOffset > lineEnd;
		return new VisualRange(startCol, endCol, includesNewline);
	}

	List<SearchMatch> visibleSearchMatches() {
		return mode == Mode.SEARCH && searchError == null && searchInput.length() > 0
				? previewSearchMatches
				: searchMatches;
	}

	int visibleActiveSearchMatch() {
		return mode == Mode.SEARCH && searchError == null && searchInput.length() > 0
				? previewSearchMatch
				: activeSearchMatch;
	}

	boolean hasSearch() {
		return searchPattern != null;
	}

	void leaveFocus() {
		if (mode == Mode.INSERT || mode == Mode.REPLACE) {
			leaveInsertMode();
		} else if (mode == Mode.SEARCH) {
			cancelSearchInput(true);
		} else {
			mode = Mode.NORMAL;
			clearPending();
		}
		command.setLength(0);
		commandCursor = 0;
		message = null;
		preferredColumn = state.cursorCol();
	}

	Result handleKey(KeyEvent key) {
		message = null;
		Result result = switch (mode) {
			case INSERT -> handleInsertKey(key);
			case REPLACE -> handleReplaceKey(key);
			case COMMAND -> handleCommandKey(key);
			case SEARCH -> handleSearchKey(key);
			case NORMAL -> handleNormalKey(key);
			case VISUAL, VISUAL_LINE, VISUAL_BLOCK -> handleVisualKey(key);
		};
		if (result.textChanged()) {
			recomputeCommittedSearch();
		}
		return result;
	}

	private Result handleInsertKey(KeyEvent key) {
		if (isEscape(key)) {
			leaveInsertMode();
			return Result.HANDLED;
		}
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			captureInsertUndo();
			state.insert('\n');
			return Result.CHANGED;
		}
		if (key.isUp()) {
			state.moveCursorUp();
			return Result.HANDLED;
		}
		if (key.isDown()) {
			state.moveCursorDown();
			return Result.HANDLED;
		}
		if (key.isLeft()) {
			state.moveCursorLeft();
			return Result.HANDLED;
		}
		if (key.isRight()) {
			state.moveCursorRight();
			return Result.HANDLED;
		}
		if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
			state.moveCursorToLineStart();
			return Result.HANDLED;
		}
		if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
			state.moveCursorToLineEnd();
			return Result.HANDLED;
		}
		if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE) {
			if (state.cursorRow() == 0 && state.cursorCol() == 0) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			state.deleteBackward();
			if (blockInsertContext != null && blockInsertedText.length() > 0) {
				blockInsertedText.deleteCharAt(blockInsertedText.length() - 1);
			}
			return changedSince(before);
		}
		if (key.isDeleteForward() || key.code() == KeyCode.DELETE) {
			if (state.cursorRow() == state.lineCount() - 1
					&& state.cursorCol() == state.getLine(state.cursorRow()).length()) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			state.deleteForward();
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('u')) {
			if (state.cursorCol() == 0) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			while (state.cursorCol() > 0) {
				state.deleteBackward();
			}
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('k')) {
			if (state.cursorCol() == state.getLine(state.cursorRow()).length()) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			while (state.cursorCol() < state.getLine(state.cursorRow()).length()) {
				state.deleteForward();
			}
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('w')) {
			if (state.cursorCol() == 0) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			deletePreviousWord();
			return changedSince(before);
		}
		if (!key.hasCtrl() && !key.hasAlt()) {
			String str = key.string();
			if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || str.charAt(0) >= 32)) {
				captureInsertUndo();
				state.insert(str);
				if (blockInsertContext != null) {
					blockInsertedText.append(str);
				}
				return Result.CHANGED;
			}
		}
		return Result.NOT_HANDLED;
	}

	private Result handleReplaceKey(KeyEvent key) {
		if (isEscape(key)) {
			leaveInsertMode();
			return Result.HANDLED;
		}
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			captureInsertUndo();
			replaceHistory.push(snapshot());
			state.insert('\n');
			return Result.CHANGED;
		}
		if (key.isUp()) {
			state.moveCursorUp();
			return Result.HANDLED;
		}
		if (key.isDown()) {
			state.moveCursorDown();
			return Result.HANDLED;
		}
		if (key.isLeft()) {
			state.moveCursorLeft();
			return Result.HANDLED;
		}
		if (key.isRight()) {
			state.moveCursorRight();
			return Result.HANDLED;
		}
		if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
			state.moveCursorToLineStart();
			return Result.HANDLED;
		}
		if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
			state.moveCursorToLineEnd();
			return Result.HANDLED;
		}
		if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE) {
			if (!replaceHistory.isEmpty()) {
				restore(replaceHistory.pop());
				return Result.CHANGED;
			}
			if (state.cursorCol() > 0) {
				state.moveCursorLeft();
			}
			return Result.HANDLED;
		}
		if (key.isDeleteForward() || key.code() == KeyCode.DELETE) {
			if (state.cursorRow() == state.lineCount() - 1
					&& state.cursorCol() == state.getLine(state.cursorRow()).length()) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			String before = state.text();
			state.deleteForward();
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('u')) {
			if (state.cursorCol() == 0) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			replaceHistory.push(snapshot());
			String before = state.text();
			while (state.cursorCol() > 0) {
				state.deleteBackward();
			}
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('k')) {
			if (state.cursorCol() == state.getLine(state.cursorRow()).length()) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			replaceHistory.push(snapshot());
			String before = state.text();
			while (state.cursorCol() < state.getLine(state.cursorRow()).length()) {
				state.deleteForward();
			}
			return changedSince(before);
		}
		if (key.hasCtrl() && key.isChar('w')) {
			if (state.cursorCol() == 0) {
				return Result.HANDLED;
			}
			captureInsertUndo();
			replaceHistory.push(snapshot());
			String before = state.text();
			deletePreviousWord();
			return changedSince(before);
		}
		if (!key.hasCtrl() && !key.hasAlt()) {
			String str = key.code() == KeyCode.TAB ? "\t" : key.string();
			if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || key.code() == KeyCode.TAB || str.charAt(0) >= 32)) {
				captureInsertUndo();
				replaceHistory.push(snapshot());
				replaceCharacterInMode(str);
				return Result.CHANGED;
			}
		}
		return Result.NOT_HANDLED;
	}

	private Result handleCommandKey(KeyEvent key) {
		if (isEscape(key)) {
			mode = Mode.NORMAL;
			command.setLength(0);
			commandCursor = 0;
			return Result.HANDLED;
		}
		if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE || (key.hasCtrl() && key.isChar('h'))) {
			if (command.length() == 0) {
				mode = Mode.NORMAL;
			} else if (commandCursor > 0) {
				int start = command.offsetByCodePoints(commandCursor, -1);
				command.delete(start, commandCursor);
				commandCursor = start;
			}
			return Result.HANDLED;
		}
		if (key.isDeleteForward() || key.code() == KeyCode.DELETE) {
			if (commandCursor < command.length()) {
				int end = command.offsetByCodePoints(commandCursor, 1);
				command.delete(commandCursor, end);
			}
			return Result.HANDLED;
		}
		if (key.isLeft()) {
			if (commandCursor > 0) {
				commandCursor = command.offsetByCodePoints(commandCursor, -1);
			}
			return Result.HANDLED;
		}
		if (key.isRight()) {
			if (commandCursor < command.length()) {
				commandCursor = command.offsetByCodePoints(commandCursor, 1);
			}
			return Result.HANDLED;
		}
		if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
			commandCursor = 0;
			return Result.HANDLED;
		}
		if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
			commandCursor = command.length();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('u')) {
			command.delete(0, commandCursor);
			commandCursor = 0;
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('k')) {
			command.delete(commandCursor, command.length());
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('w')) {
			deletePreviousWordInCommand();
			return Result.HANDLED;
		}
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			String entered = command.toString();
			command.setLength(0);
			commandCursor = 0;
			mode = Mode.NORMAL;
			if (entered.equals("q")) {
				return Result.SUBMIT;
			}
			if (entered.equals("noh") || entered.equals("nohlsearch")) {
				clearQuerySearch();
				return Result.HANDLED;
			}
			Result fileResult = handleFileCommand(entered);
			if (fileResult != null) {
				return fileResult;
			}
			message = "Not an editor command: " + entered;
			return Result.HANDLED;
		}
		if (!key.hasCtrl() && !key.hasAlt()) {
			String str = key.string();
			if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || str.charAt(0) >= 32)) {
				command.insert(commandCursor, str);
				commandCursor += str.length();
				return Result.HANDLED;
			}
		}
		return Result.HANDLED;
	}

	@Nullable
	private Result handleFileCommand(String entered) {
		String trimmed = entered.strip();
		@Var int separator = 0;
		while (separator < trimmed.length() && !Character.isWhitespace(trimmed.charAt(separator))) {
			separator++;
		}
		@Var String name = trimmed.substring(0, separator);
		boolean force = name.endsWith("!");
		if (force) {
			name = name.substring(0, name.length() - 1);
		}
		boolean write = name.equals("w") || name.equals("write");
		boolean saveAs = name.equals("saveas");
		boolean read = name.equals("r") || name.equals("read");
		boolean edit = name.equals("e") || name.equals("edit");
		if (!write && !saveAs && !read && !edit) {
			return null;
		}
		if (read && force) {
			message = ":" + name + "! is not supported";
			return Result.HANDLED;
		}
		String argument;
		try {
			argument = parseFileArgument(trimmed.substring(separator));
		} catch (IllegalArgumentException e) {
			message = e.getMessage();
			return Result.HANDLED;
		}
		if (argument.isEmpty() && (saveAs || read)) {
			message = "File name required";
			return Result.HANDLED;
		}
		Path file;
		try {
			file = argument.isEmpty() ? currentFile : Path.of(argument);
		} catch (InvalidPathException e) {
			message = "Invalid file name: " + argument;
			return Result.HANDLED;
		}
		if (file == null) {
			message = "No current file";
			return Result.HANDLED;
		}
		try {
			if (read) {
				return readFile(file);
			}
			if (edit) {
				return editFile(file, force);
			}
			return writeFile(file, force, saveAs);
		} catch (FileAlreadyExistsException e) {
			message = "File already exists (use ! to overwrite): " + file;
			return Result.HANDLED;
		} catch (IOException | SecurityException e) {
			message = "File error: " + e.getMessage();
			return Result.HANDLED;
		}
	}

	private static String parseFileArgument(String raw) {
		StringBuilder path = new StringBuilder();
		@Var boolean escaped = false;
		@Var int pendingSpaces = 0;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (escaped) {
				path.append(" ".repeat(pendingSpaces)).append(c);
				pendingSpaces = 0;
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else if (Character.isWhitespace(c)) {
				if (!path.isEmpty()) {
					pendingSpaces++;
				}
			} else {
				path.append(" ".repeat(pendingSpaces)).append(c);
				pendingSpaces = 0;
			}
		}
		if (escaped) {
			throw new IllegalArgumentException("Trailing backslash in file name");
		}
		return path.toString();
	}

	private Result writeFile(Path file, boolean force, boolean saveAs) throws IOException {
		boolean current = currentFile != null && currentFile.toAbsolutePath().normalize().equals(file.toAbsolutePath().normalize());
		if (force || current) {
			Files.writeString(file, state.text(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} else {
			Files.writeString(file, state.text(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		}
		if (saveAs) {
			currentFile = file;
		}
		if (current || saveAs) {
			savedText = state.text();
		}
		message = "Written: " + file;
		return Result.HANDLED;
	}

	private Result editFile(Path file, boolean force) throws IOException {
		if (!force && !state.text().equals(savedText)) {
			message = "Unsaved changes (use :e! to discard them)";
			return Result.HANDLED;
		}
		String text = Files.readString(file, StandardCharsets.UTF_8);
		boolean changed = !state.text().equals(text);
		setTextAndPosition(text, 0, 0);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		undoStack.clear();
		replaceHistory.clear();
		currentFile = file;
		savedText = text;
		message = "Opened: " + file;
		return changed ? Result.CHANGED : Result.HANDLED;
	}

	private Result readFile(Path file) throws IOException {
		String text = Files.readString(file, StandardCharsets.UTF_8);
		if (text.isEmpty()) {
			message = "Read: " + file;
			return Result.HANDLED;
		}
		String inserted = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
		int row = state.cursorRow();
		List<String> lines = lines();
		lines.addAll(row + 1, Arrays.asList(inserted.split("\n", -1)));
		pushUndo(snapshot());
		setTextAndPosition(String.join("\n", lines), row + 1, 0);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		message = "Read: " + file;
		return Result.CHANGED;
	}

	private Result handleSearchKey(KeyEvent key) {
		if (key.hasCtrl() && key.isChar('l')) {
			clearQuerySearch();
			mode = Mode.NORMAL;
			return Result.HANDLED;
		}
		if (isEscape(key)) {
			cancelSearchInput(true);
			return Result.HANDLED;
		}
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			if (searchInput.length() == 0) {
				cancelSearchInput(true);
				return navigateQuerySearch(true, 1);
			}
			if (searchError != null) {
				return Result.HANDLED;
			}
			searchPattern = searchInput.toString();
			searchMatches = previewSearchMatches;
			activeSearchMatch = previewSearchMatch;
			finishSearchInput();
			return Result.HANDLED;
		}
		if (key.isDeleteBackward() || key.code() == KeyCode.BACKSPACE || (key.hasCtrl() && key.isChar('h'))) {
			if (searchInputCursor > 0) {
				int start = searchInput.offsetByCodePoints(searchInputCursor, -1);
				searchInput.delete(start, searchInputCursor);
				searchInputCursor = start;
				updateSearchPreview();
			}
			return Result.HANDLED;
		}
		if (key.isDeleteForward() || key.code() == KeyCode.DELETE) {
			if (searchInputCursor < searchInput.length()) {
				int end = searchInput.offsetByCodePoints(searchInputCursor, 1);
				searchInput.delete(searchInputCursor, end);
				updateSearchPreview();
			}
			return Result.HANDLED;
		}
		if (key.isLeft()) {
			if (searchInputCursor > 0) {
				searchInputCursor = searchInput.offsetByCodePoints(searchInputCursor, -1);
			}
			return Result.HANDLED;
		}
		if (key.isRight()) {
			if (searchInputCursor < searchInput.length()) {
				searchInputCursor = searchInput.offsetByCodePoints(searchInputCursor, 1);
			}
			return Result.HANDLED;
		}
		if (key.isHome() || (key.hasCtrl() && key.isChar('a'))) {
			searchInputCursor = 0;
			return Result.HANDLED;
		}
		if (key.isEnd() || (key.hasCtrl() && key.isChar('e'))) {
			searchInputCursor = searchInput.length();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('u')) {
			searchInput.delete(0, searchInputCursor);
			searchInputCursor = 0;
			updateSearchPreview();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('k')) {
			searchInput.delete(searchInputCursor, searchInput.length());
			updateSearchPreview();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('w')) {
			deletePreviousWordInSearch();
			updateSearchPreview();
			return Result.HANDLED;
		}
		if (!key.hasCtrl() && !key.hasAlt()) {
			String str = key.string();
			if (str != null && !str.isEmpty() && (key.code() == KeyCode.CHAR || str.charAt(0) >= 32)) {
				searchInput.insert(searchInputCursor, str);
				searchInputCursor += str.length();
				updateSearchPreview();
			}
		}
		return Result.HANDLED;
	}

	private Result handleNormalKey(KeyEvent key) {
		if (isEscape(key)) {
			clearPending();
			return Result.HANDLED;
		}
		if (awaitingCharacterMotion != null) {
			if (key.hasCtrl() || key.hasAlt()) {
				clearPending();
				return Result.HANDLED;
			}
			String target = key.string();
			if (target == null || target.isEmpty()) {
				clearPending();
				return Result.HANDLED;
			}
			return completeCharacterMotion(target);
		}
		if (awaitingReplace) {
			if (key.hasCtrl() || key.hasAlt()) {
				clearPending();
				return Result.HANDLED;
			}
			if (key.isConfirm() || key.code() == KeyCode.ENTER) {
				return completeReplace("\n");
			}
			if (key.code() == KeyCode.TAB) {
				return completeReplace("\t");
			}
			String target = key.string();
			if (target != null && !target.isEmpty()) {
				if ("\r".equals(target) || "\n".equals(target)) {
					return completeReplace("\n");
				}
				if (key.code() == KeyCode.CHAR || target.charAt(0) >= 32) {
					return completeReplace(target);
				}
			}
			clearPending();
			return Result.HANDLED;
		}
		if ((pendingOperator != null || pendingG) && key.code() != KeyCode.CHAR) {
			clearPending();
			return Result.HANDLED;
		}
		if (key.isUp()) {
			return executeStandaloneMotion(Motion.LINE_UP, consumeCount(), null);
		}
		if (key.isDown()) {
			return executeStandaloneMotion(Motion.LINE_DOWN, consumeCount(), null);
		}
		if (key.isLeft()) {
			return executeStandaloneMotion(Motion.LEFT, consumeCount(), null);
		}
		if (key.isRight()) {
			return executeStandaloneMotion(Motion.RIGHT, consumeCount(), null);
		}
		if (key.isHome()) {
			return executeStandaloneMotion(Motion.LINE_START, 1, null);
		}
		if (key.isEnd()) {
			return executeStandaloneMotion(Motion.LINE_END, 1, null);
		}
		if (key.hasCtrl() && key.isChar('l')) {
			clearQuerySearch();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isCharIgnoreCase('v')) {
			return enterVisualMode(Mode.VISUAL_BLOCK);
		}
		if (key.hasShift() && key.isCharIgnoreCase('v')) {
			return enterVisualMode(Mode.VISUAL_LINE);
		}
		if (key.hasCtrl() || key.hasAlt()) {
			return Result.NOT_HANDLED;
		}
		String str = key.string();
		if (str == null || str.length() != 1) {
			return Result.NOT_HANDLED;
		}
		char ch = str.charAt(0);
		if ((ch >= '1' && ch <= '9') || (ch == '0' && count.length() > 0)) {
			count.append(ch);
			return Result.HANDLED;
		}

		if (pendingG) {
			return handlePendingG(ch);
		}
		if (pendingOperator != null) {
			return handleOperatorMotion(ch);
		}

		return switch (ch) {
			case 'h' -> executeStandaloneMotion(Motion.LEFT, consumeCount(), null);
			case 'j' -> executeStandaloneMotion(Motion.LINE_DOWN, consumeCount(), null);
			case 'k' -> executeStandaloneMotion(Motion.LINE_UP, consumeCount(), null);
			case 'l' -> executeStandaloneMotion(Motion.RIGHT, consumeCount(), null);
			case 'w' -> executeStandaloneMotion(Motion.WORD_FORWARD, consumeCount(), null);
			case 'b' -> executeStandaloneMotion(Motion.WORD_BACKWARD, consumeCount(), null);
			case 'e' -> executeStandaloneMotion(Motion.WORD_END, consumeCount(), null);
			case '0' -> executeStandaloneMotion(Motion.LINE_START, 1, null);
			case '$' -> executeStandaloneMotion(Motion.LINE_END, consumeCount(), null);
			case 'g' -> startGPrefix();
			case 'd' -> startOperator(Operator.DELETE);
			case 'c' -> startOperator(Operator.CHANGE);
			case 'y' -> startOperator(Operator.YANK);
			case 'G' -> executeStandaloneDocumentMotion(Motion.LAST_LINE);
			case '%' -> executeStandaloneMotion(Motion.MATCHING_DELIMITER, consumeCount(), null);
			case 'f' -> awaitCharacter(Motion.FIND_FORWARD);
			case 'F' -> awaitCharacter(Motion.FIND_BACKWARD);
			case 't' -> awaitCharacter(Motion.TILL_FORWARD);
			case 'T' -> awaitCharacter(Motion.TILL_BACKWARD);
			case ';' -> repeatLastSearch(false);
			case ',' -> repeatLastSearch(true);
			case 'i' -> enterInsertAtCursor();
			case 'a' -> enterInsertAfterCursor();
			case 'I' -> enterInsertAtLineStart();
			case 'A' -> enterInsertAtLineEnd();
			case 'o' -> openLine(true);
			case 'O' -> openLine(false);
			case 'x' -> deleteCharacters(false, consumeCount());
			case 'X' -> deleteCharacters(true, consumeCount());
			case 'r' -> startReplace();
			case 'R' -> enterReplaceMode();
			case 'D' -> deleteToLineEnd(false);
			case 'C' -> deleteToLineEnd(true);
			case 'p' -> paste(true, consumeCount());
			case 'P' -> paste(false, consumeCount());
			case 'u' -> undo(consumeCount());
			case '/' -> enterSearchMode();
			case 'n' -> navigateQuerySearch(true, consumeCount());
			case 'N' -> navigateQuerySearch(false, consumeCount());
			case 'v' -> enterVisualMode(Mode.VISUAL);
			case 'V' -> enterVisualMode(Mode.VISUAL_LINE);
			case ':' -> enterCommandMode();
			default -> cancelPending();
		};
	}

	private Result enterVisualMode(Mode targetMode) {
		clearPending();
		mode = targetMode;
		visualAnchorRow = state.cursorRow();
		visualAnchorCol = state.cursorCol();
		return Result.HANDLED;
	}

	private Result handleVisualKey(KeyEvent key) {
		if (isEscape(key)) {
			mode = Mode.NORMAL;
			preferredColumn = state.cursorCol();
			clearPending();
			return Result.HANDLED;
		}
		if (awaitingCharacterMotion != null) {
			if (key.hasCtrl() || key.hasAlt()) {
				clearPending();
				return Result.HANDLED;
			}
			String target = key.string();
			if (target == null || target.isEmpty()) {
				clearPending();
				return Result.HANDLED;
			}
			return completeVisualCharacterMotion(target);
		}
		if (awaitingReplace) {
			if (key.hasCtrl() || key.hasAlt()) {
				clearPending();
				return Result.HANDLED;
			}
			if (key.isConfirm() || key.code() == KeyCode.ENTER) {
				return completeVisualReplace("\n");
			}
			if (key.code() == KeyCode.TAB) {
				return completeVisualReplace("\t");
			}
			String target = key.string();
			if (target != null && !target.isEmpty()) {
				if ("\r".equals(target) || "\n".equals(target)) {
					return completeVisualReplace("\n");
				}
				if (key.code() == KeyCode.CHAR || target.charAt(0) >= 32) {
					return completeVisualReplace(target);
				}
			}
			clearPending();
			return Result.HANDLED;
		}
		if (key.isUp()) {
			return executeVisualMotion(Motion.LINE_UP);
		}
		if (key.isDown()) {
			return executeVisualMotion(Motion.LINE_DOWN);
		}
		if (key.isLeft()) {
			return executeVisualMotion(Motion.LEFT);
		}
		if (key.isRight()) {
			return executeVisualMotion(Motion.RIGHT);
		}
		if (key.isHome()) {
			return executeVisualMotion(Motion.LINE_START);
		}
		if (key.isEnd()) {
			return executeVisualMotion(Motion.LINE_END);
		}
		if (key.isConfirm() || key.code() == KeyCode.ENTER) {
			return executeVisualMotion(Motion.LINE_DOWN);
		}
		if (key.hasCtrl() && key.isCharIgnoreCase('v')) {
			if (mode == Mode.VISUAL_BLOCK) {
				mode = Mode.NORMAL;
				preferredColumn = state.cursorCol();
			} else {
				mode = Mode.VISUAL_BLOCK;
			}
			clearPending();
			return Result.HANDLED;
		}
		if (key.hasShift() && key.isCharIgnoreCase('v')) {
			if (mode == Mode.VISUAL_LINE) {
				mode = Mode.NORMAL;
				preferredColumn = state.cursorCol();
			} else {
				mode = Mode.VISUAL_LINE;
			}
			clearPending();
			return Result.HANDLED;
		}
		if (key.hasCtrl() && key.isChar('l')) {
			clearQuerySearch();
			return Result.HANDLED;
		}
		if (key.hasCtrl() || key.hasAlt()) {
			return Result.NOT_HANDLED;
		}
		String str = key.string();
		if (str == null || str.length() != 1) {
			return Result.NOT_HANDLED;
		}
		char ch = str.charAt(0);
		if ((ch >= '1' && ch <= '9') || (ch == '0' && count.length() > 0)) {
			count.append(ch);
			return Result.HANDLED;
		}
		if (pendingG) {
			return handleVisualPendingG(ch);
		}
		if (pendingTextObjectScope != null) {
			return completeVisualTextObject(ch);
		}

		return switch (ch) {
			case 'v' -> {
				if (mode == Mode.VISUAL) {
					mode = Mode.NORMAL;
					preferredColumn = state.cursorCol();
				} else {
					mode = Mode.VISUAL;
				}
				clearPending();
				yield Result.HANDLED;
			}
			case 'V' -> {
				if (mode == Mode.VISUAL_LINE) {
					mode = Mode.NORMAL;
					preferredColumn = state.cursorCol();
				} else {
					mode = Mode.VISUAL_LINE;
				}
				clearPending();
				yield Result.HANDLED;
			}
			case 'h' -> executeVisualMotion(Motion.LEFT);
			case 'j' -> executeVisualMotion(Motion.LINE_DOWN);
			case 'k' -> executeVisualMotion(Motion.LINE_UP);
			case 'l' -> executeVisualMotion(Motion.RIGHT);
			case 'w' -> executeVisualMotion(Motion.WORD_FORWARD);
			case 'b' -> executeVisualMotion(Motion.WORD_BACKWARD);
			case 'e' -> executeVisualMotion(Motion.WORD_END);
			case '0' -> executeVisualMotion(Motion.LINE_START);
			case '$' -> executeVisualMotion(Motion.LINE_END);
			case 'g' -> startGPrefix();
			case 'G' -> executeVisualDocumentMotion(Motion.LAST_LINE);
			case '%' -> executeVisualMotion(Motion.MATCHING_DELIMITER);
			case 'f' -> awaitCharacter(Motion.FIND_FORWARD);
			case 'F' -> awaitCharacter(Motion.FIND_BACKWARD);
			case 't' -> awaitCharacter(Motion.TILL_FORWARD);
			case 'T' -> awaitCharacter(Motion.TILL_BACKWARD);
			case ';' -> repeatLastSearch(false);
			case ',' -> repeatLastSearch(true);
			case 'o' -> toggleVisualEndpoint(false);
			case 'O' -> toggleVisualEndpoint(true);
			case 'i' -> startTextObject(TextObjectScope.INNER);
			case 'a' -> startTextObject(TextObjectScope.AROUND);
			case 'y' -> yankVisualSelection();
			case 'd', 'x' -> deleteVisualSelection();
			case 'c', 's' -> changeVisualSelection();
			case 'I' -> {
				if (mode == Mode.VISUAL_BLOCK) {
					yield beginBlockInsert(false);
				}
				yield executeVisualMotion(Motion.LINE_START);
			}
			case 'A' -> {
				if (mode == Mode.VISUAL_BLOCK) {
					yield beginBlockInsert(true);
				}
				yield executeVisualMotion(Motion.LINE_END);
			}
			case 'p', 'P' -> pasteVisualSelection();
			case 'r' -> {
				awaitingReplace = true;
				yield Result.HANDLED;
			}
			case '>' -> indentVisualSelection(true);
			case '<' -> indentVisualSelection(false);
			case '~' -> toggleCaseVisualSelection();
			case 'u' -> convertCaseVisualSelection(false);
			case 'U' -> convertCaseVisualSelection(true);
			case 'J' -> joinVisualSelection();
			case ':' -> enterCommandMode();
			default -> cancelPending();
		};
	}

	private Result handleVisualPendingG(char ch) {
		if (ch != 'g') {
			clearPending();
			return Result.HANDLED;
		}
		pendingG = false;
		Integer target = consumeOptionalCount();
		MotionResult result = resolveMotion(Motion.FIRST_LINE, 1, target == null ? 1 : target, null);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		return moveToVisual(result);
	}

	private Result executeVisualMotion(Motion motion) {
		int repetitions = consumeCount();
		MotionResult result = resolveMotion(motion, repetitions, null, null);
		if (result == null) {
			clearPending();
			if (motion == Motion.LINE_END) {
				preferredColumn = Integer.MAX_VALUE;
			}
			return Result.HANDLED;
		}
		Result res = moveToVisual(result);
		if (motion == Motion.LINE_END) {
			preferredColumn = Integer.MAX_VALUE;
		} else if (motion != Motion.LINE_DOWN && motion != Motion.LINE_UP) {
			preferredColumn = state.cursorCol();
		}
		return res;
	}

	private Result executeVisualDocumentMotion(Motion motion) {
		Integer target = consumeOptionalCount();
		MotionResult result = resolveMotion(motion, 1, target, null);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		Result res = moveToVisual(result);
		preferredColumn = state.cursorCol();
		return res;
	}

	private Result moveToVisual(MotionResult motion) {
		setPositionFromOffset(motion.targetOffset());
		normalizeNormalCursor();
		clearPending();
		return Result.HANDLED;
	}

	private Result completeVisualCharacterMotion(String target) {
		Motion motion = awaitingCharacterMotion;
		awaitingCharacterMotion = null;
		if (motion == null) {
			return Result.HANDLED;
		}
		int repetitions = consumeCount();
		MotionResult result = resolveMotion(motion, repetitions, null, target);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		lastSearch = new CharacterSearch(motion, target);
		Result res = moveToVisual(result);
		preferredColumn = state.cursorCol();
		return res;
	}

	private Result toggleVisualEndpoint(boolean sameLine) {
		clearPending();
		if (mode == Mode.VISUAL_BLOCK && sameLine) {
			int curCol = state.cursorCol();
			int targetCol = visualAnchorCol;
			visualAnchorCol = curCol;
			setPosition(state.cursorRow(), targetCol);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			return Result.HANDLED;
		}
		int curRow = state.cursorRow();
		int curCol = state.cursorCol();
		setPosition(visualAnchorRow, visualAnchorCol);
		visualAnchorRow = curRow;
		visualAnchorCol = curCol;
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		return Result.HANDLED;
	}

	private Result completeVisualTextObject(char key) {
		TextObjectScope scope = Objects.requireNonNull(pendingTextObjectScope);
		pendingTextObjectScope = null;
		TextObjectRange range;
		if (key == 'w') {
			range = resolveWordTextObject(scope, consumeCount());
		} else {
			Character opening = textObjectOpening(key);
			if (opening == null) {
				return cancelPending();
			}
			range = resolveTextObject(opening, scope, consumeCount());
		}
		if (range == null || range.start() == range.end()) {
			return cancelPending();
		}
		int[] startPos = positionForOffset(state.text(), range.start());
		int endCharOffset = Math.max(range.start(), previousOffset(range.end()));
		int[] endPos = positionForOffset(state.text(), endCharOffset);
		visualAnchorRow = startPos[0];
		visualAnchorCol = startPos[1];
		mode = Mode.VISUAL;
		setPosition(endPos[0], endPos[1]);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.HANDLED;
	}

	private Result beginBlockInsert(boolean append) {
		int minRow = Math.min(visualAnchorRow, state.cursorRow());
		int maxRow = Math.max(visualAnchorRow, state.cursorRow());
		int minCol = Math.min(visualAnchorCol, state.cursorCol());
		int maxCol = Math.max(visualAnchorCol, state.cursorCol());
		int insertCol = append ? maxCol + 1 : minCol;
		blockInsertContext = new BlockInsertContext(minRow, maxRow, insertCol, append);
		blockInsertedText.setLength(0);
		setPosition(minRow, insertCol);
		beginInsert(false);
		return Result.HANDLED;
	}

	private Result completeVisualReplace(String target) {
		clearPending();
		Snapshot before = snapshot();
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				if (minCol < line.length()) {
					int end = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					String replacement = target.repeat(end - minCol);
					lines.set(r, line.substring(0, minCol) + replacement + line.substring(end));
				}
			}
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			return Result.CHANGED;
		}
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				lines.set(r, target.repeat(line.length()));
			}
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, 0);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			return Result.CHANGED;
		}
		int[] range = characterwiseSelectionOffsets();
		String text = state.text();
		StringBuilder sb = new StringBuilder();
		for (int i = range[0]; i < range[1]; i++) {
			char c = text.charAt(i);
			sb.append(c == '\n' ? '\n' : target);
		}
		String newText = text.substring(0, range[0]) + sb + text.substring(range[1]);
		pushUndo(before);
		mode = Mode.NORMAL;
		int[] pos = positionForOffset(newText, range[0]);
		setTextAndPosition(newText, pos[0], pos[1]);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		return Result.CHANGED;
	}

	private int[] characterwiseSelectionOffsets() {
		String text = state.text();
		if (text.isEmpty()) {
			return new int[] { 0, 0 };
		}
		int anchor = offsetForPosition(visualAnchorRow, visualAnchorCol);
		int cursor = offset();
		int start = Math.min(anchor, cursor);
		int endChar = Math.max(anchor, cursor);
		@Var int end = nextOffset(endChar);
		if (end == endChar && endChar < text.length()) {
			end = Math.min(endChar + 1, text.length());
		}
		return new int[] { Math.min(start, text.length()), Math.min(end, text.length()) };
	}

	private void yankVisualSelectionSilently() {
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			int end = Math.min(lines.size(), maxRow + 1);
			String joined = String.join("\n", lines.subList(minRow, end));
			register = new Register(joined, RegisterType.LINEWISE, 0);
		} else if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> blockLines = new ArrayList<>();
			@Var int blockWidth = 0;
			for (int r = minRow; r <= maxRow && r < state.lineCount(); r++) {
				String line = state.getLine(r);
				if (minCol < line.length()) {
					int colEnd = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					blockLines.add(line.substring(minCol, colEnd));
					blockWidth = Math.max(blockWidth, colEnd - minCol);
				} else {
					blockLines.add("");
				}
			}
			int width = preferredColumn == Integer.MAX_VALUE ? blockWidth : maxCol - minCol + 1;
			register = new Register(String.join("\n", blockLines), RegisterType.BLOCKWISE, width);
		} else {
			int[] range = characterwiseSelectionOffsets();
			String selected = state.text().substring(range[0], range[1]);
			register = new Register(selected, RegisterType.CHARACTERWISE, 0);
		}
	}

	private Result yankVisualSelection() {
		yankVisualSelectionSilently();
		int targetRow = Math.min(visualAnchorRow, state.cursorRow());
		int targetCol = mode == Mode.VISUAL_LINE ? 0 : Math.min(visualAnchorCol, state.cursorCol());
		mode = Mode.NORMAL;
		setPosition(targetRow, targetCol);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.HANDLED;
	}

	private Result deleteVisualSelection() {
		Snapshot before = snapshot();
		yankVisualSelectionSilently();
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			int end = Math.min(lines.size(), maxRow + 1);
			lines.subList(minRow, end).clear();
			if (lines.isEmpty()) {
				lines.add("");
			}
			int row = Math.min(minRow, lines.size() - 1);
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), row, 0);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			clearPending();
			return Result.CHANGED;
		}
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				if (minCol < line.length()) {
					int colEnd = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					lines.set(r, line.substring(0, minCol) + line.substring(colEnd));
				}
			}
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			clearPending();
			return Result.CHANGED;
		}
		int[] range = characterwiseSelectionOffsets();
		String text = state.text();
		String newText = text.substring(0, range[0]) + text.substring(range[1]);
		pushUndo(before);
		mode = Mode.NORMAL;
		int[] pos = positionForOffset(newText, range[0]);
		setTextAndPosition(newText, pos[0], pos[1]);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result changeVisualSelection() {
		Snapshot before = snapshot();
		yankVisualSelectionSilently();
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			int end = Math.min(lines.size(), maxRow + 1);
			lines.subList(minRow, end).clear();
			lines.add(minRow, "");
			pushUndo(before);
			setTextAndPosition(String.join("\n", lines), minRow, 0);
			beginInsert(true);
			return Result.CHANGED;
		}
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				if (minCol < line.length()) {
					int colEnd = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					lines.set(r, line.substring(0, minCol) + line.substring(colEnd));
				}
			}
			pushUndo(before);
			blockInsertContext = new BlockInsertContext(minRow, maxRow, minCol, false);
			blockInsertedText.setLength(0);
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
			beginInsert(true);
			return Result.CHANGED;
		}
		int[] range = characterwiseSelectionOffsets();
		String text = state.text();
		String newText = text.substring(0, range[0]) + text.substring(range[1]);
		pushUndo(before);
		int[] pos = positionForOffset(newText, range[0]);
		setTextAndPosition(newText, pos[0], pos[1]);
		beginInsert(true);
		return Result.CHANGED;
	}

	private Result pasteVisualSelection() {
		if (register.text().isEmpty() && !register.linewise() && !register.blockwise()) {
			return deleteVisualSelection();
		}
		Snapshot before = snapshot();
		Register toPaste = register;
		yankVisualSelectionSilently();
		if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			int end = Math.min(lines.size(), maxRow + 1);
			lines.subList(minRow, end).clear();
			List<String> pastedLines = Arrays.asList(toPaste.text().split("\n", -1));
			lines.addAll(minRow, pastedLines);
			if (lines.isEmpty()) {
				lines.add("");
			}
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, 0);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			clearPending();
			return Result.CHANGED;
		}
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			List<String> pastedLines = Arrays.asList(toPaste.text().split("\n", -1));
			for (int i = 0; i <= maxRow - minRow; i++) {
				int r = minRow + i;
				if (r >= lines.size()) {
					break;
				}
				String line = lines.get(r);
				String part = i < pastedLines.size() ? pastedLines.get(i) : "";
				int colEnd = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
				String prefix = minCol < line.length() ? line.substring(0, minCol) : line;
				String suffix = colEnd < line.length() ? line.substring(colEnd) : "";
				lines.set(r, prefix + part + suffix);
			}
			pushUndo(before);
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			clearPending();
			return Result.CHANGED;
		}
		int[] range = characterwiseSelectionOffsets();
		String text = state.text();
		String newText = text.substring(0, range[0]) + toPaste.text() + text.substring(range[1]);
		pushUndo(before);
		mode = Mode.NORMAL;
		int[] pos = positionForOffset(newText, range[0]);
		setTextAndPosition(newText, pos[0], pos[1]);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result indentVisualSelection(boolean right) {
		pushUndo(snapshot());
		int minRow = Math.min(visualAnchorRow, state.cursorRow());
		int maxRow = Math.max(visualAnchorRow, state.cursorRow());
		List<String> lines = lines();
		for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
			String line = lines.get(r);
			if (right) {
				lines.set(r, "  " + line);
			} else {
				if (line.startsWith("  ")) {
					lines.set(r, line.substring(2));
				} else if (line.startsWith(" ")) {
					lines.set(r, line.substring(1));
				}
			}
		}
		mode = Mode.NORMAL;
		setTextAndPosition(String.join("\n", lines), minRow, 0);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result convertCaseVisualSelection(boolean upper) {
		pushUndo(snapshot());
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				if (minCol < line.length()) {
					int end = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					String segment = line.substring(minCol, end);
					String converted = upper ? segment.toUpperCase(Locale.ROOT) : segment.toLowerCase(Locale.ROOT);
					lines.set(r, line.substring(0, minCol) + converted + line.substring(end));
				}
			}
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
		} else if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				lines.set(r, upper ? line.toUpperCase(Locale.ROOT) : line.toLowerCase(Locale.ROOT));
			}
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, 0);
		} else {
			int[] range = characterwiseSelectionOffsets();
			String text = state.text();
			String segment = text.substring(range[0], range[1]);
			String converted = upper ? segment.toUpperCase(Locale.ROOT) : segment.toLowerCase(Locale.ROOT);
			String newText = text.substring(0, range[0]) + converted + text.substring(range[1]);
			mode = Mode.NORMAL;
			int[] pos = positionForOffset(newText, range[0]);
			setTextAndPosition(newText, pos[0], pos[1]);
		}
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result toggleCaseVisualSelection() {
		pushUndo(snapshot());
		if (mode == Mode.VISUAL_BLOCK) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			int minCol = Math.min(visualAnchorCol, state.cursorCol());
			int maxCol = preferredColumn == Integer.MAX_VALUE
					? Integer.MAX_VALUE
					: Math.max(visualAnchorCol, state.cursorCol());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				if (minCol < line.length()) {
					int end = Math.min(maxCol == Integer.MAX_VALUE ? line.length() : maxCol + 1, line.length());
					String segment = line.substring(minCol, end);
					StringBuilder toggled = new StringBuilder();
					for (int i = 0; i < segment.length(); i++) {
						char c = segment.charAt(i);
						toggled.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
					}
					lines.set(r, line.substring(0, minCol) + toggled + line.substring(end));
				}
			}
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, minCol);
		} else if (mode == Mode.VISUAL_LINE) {
			int minRow = Math.min(visualAnchorRow, state.cursorRow());
			int maxRow = Math.max(visualAnchorRow, state.cursorRow());
			List<String> lines = lines();
			for (int r = minRow; r <= maxRow && r < lines.size(); r++) {
				String line = lines.get(r);
				StringBuilder toggled = new StringBuilder();
				for (int i = 0; i < line.length(); i++) {
					char c = line.charAt(i);
					toggled.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
				}
				lines.set(r, toggled.toString());
			}
			mode = Mode.NORMAL;
			setTextAndPosition(String.join("\n", lines), minRow, 0);
		} else {
			int[] range = characterwiseSelectionOffsets();
			String text = state.text();
			StringBuilder sb = new StringBuilder();
			for (int i = range[0]; i < range[1]; i++) {
				char c = text.charAt(i);
				sb.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
			}
			String newText = text.substring(0, range[0]) + sb + text.substring(range[1]);
			mode = Mode.NORMAL;
			int[] pos = positionForOffset(newText, range[0]);
			setTextAndPosition(newText, pos[0], pos[1]);
		}
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result joinVisualSelection() {
		pushUndo(snapshot());
		int minRow = Math.min(visualAnchorRow, state.cursorRow());
		@Var int maxRow = Math.max(visualAnchorRow, state.cursorRow());
		if (minRow == maxRow && maxRow < state.lineCount() - 1) {
			maxRow++;
		}
		List<String> lines = lines();
		StringBuilder sb = new StringBuilder(lines.get(minRow));
		for (int r = minRow + 1; r <= maxRow && r < lines.size(); r++) {
			String trimmed = lines.get(r).stripLeading();
			if (sb.length() > 0 && !trimmed.isEmpty()) {
				sb.append(' ');
			}
			sb.append(trimmed);
		}
		int end = Math.min(lines.size(), maxRow + 1);
		lines.subList(minRow, end).clear();
		lines.add(minRow, sb.toString());
		mode = Mode.NORMAL;
		setTextAndPosition(String.join("\n", lines), minRow, 0);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.CHANGED;
	}

	private Result startOperator(Operator operator) {
		operatorCount = consumeCount();
		pendingOperator = operator;
		return Result.HANDLED;
	}

	private Result startGPrefix() {
		pendingG = true;
		return Result.HANDLED;
	}

	private Result handlePendingG(char ch) {
		if (ch != 'g') {
			clearPending();
			return Result.HANDLED;
		}
		pendingG = false;
		if (pendingOperator == null) {
			Integer target = consumeOptionalCount();
			return executeStandaloneMotion(Motion.FIRST_LINE, 1, target == null ? 1 : target);
		}
		return executeOperatorDocumentMotion(Motion.FIRST_LINE);
	}

	private Result handleOperatorMotion(char ch) {
		Operator operator = pendingOperator;
		if (operator == null) {
			return Result.HANDLED;
		}
		if (pendingTextObjectScope != null) {
			return completeTextObject(ch);
		}
		if (ch == operatorKey(operator)) {
			return applyLinewiseOperator(operator, multipliedCount(operatorCount, consumeCount()), state.cursorRow());
		}
		return switch (ch) {
			case 'h' -> executeOperatorMotion(Motion.LEFT);
			case 'j' -> executeOperatorMotion(Motion.LINE_DOWN);
			case 'k' -> executeOperatorMotion(Motion.LINE_UP);
			case 'l' -> executeOperatorMotion(Motion.RIGHT);
			case 'w' -> {
				if (pendingTextObjectScope != null) {
					yield completeWordTextObject();
				}
				yield executeOperatorMotion(operator == Operator.CHANGE && isOnWord() ? Motion.WORD_END : Motion.WORD_FORWARD);
			}
			case 'b' -> executeOperatorMotion(Motion.WORD_BACKWARD);
			case 'e' -> executeOperatorMotion(Motion.WORD_END);
			case '0' -> executeOperatorMotion(Motion.LINE_START);
			case '$' -> executeOperatorMotion(Motion.LINE_END);
			case 'g' -> startGPrefix();
			case 'G' -> executeOperatorDocumentMotion(Motion.LAST_LINE);
			case '%' -> executeOperatorMotion(Motion.MATCHING_DELIMITER);
			case 'i' -> startTextObject(TextObjectScope.INNER);
			case 'a' -> startTextObject(TextObjectScope.AROUND);
			case 'f' -> awaitCharacter(Motion.FIND_FORWARD);
			case 'F' -> awaitCharacter(Motion.FIND_BACKWARD);
			case 't' -> awaitCharacter(Motion.TILL_FORWARD);
			case 'T' -> awaitCharacter(Motion.TILL_BACKWARD);
			case ';' -> repeatLastSearch(false);
			case ',' -> repeatLastSearch(true);
			default -> cancelPending();
		};
	}

	private Result startTextObject(TextObjectScope scope) {
		pendingTextObjectScope = scope;
		return Result.HANDLED;
	}

	private Result completeTextObject(char key) {
		if (key == 'w') {
			return completeWordTextObject();
		}
		Character opening = textObjectOpening(key);
		if (opening == null) {
			return cancelPending();
		}
		Operator operator = Objects.requireNonNull(pendingOperator);
		TextObjectScope scope = Objects.requireNonNull(pendingTextObjectScope);
		int repetitions = multipliedCount(operatorCount, consumeCount());
		TextObjectRange range = resolveTextObject(opening, scope, repetitions);
		if (range == null || range.start() == range.end()) {
			return cancelPending();
		}
		return applyCharacterwiseOperator(operator, range.start(), range.end());
	}

	private Result completeWordTextObject() {
		Operator operator = Objects.requireNonNull(pendingOperator);
		TextObjectScope scope = Objects.requireNonNull(pendingTextObjectScope);
		TextObjectRange range = resolveWordTextObject(scope, 1);
		if (range == null || range.start() == range.end()) {
			return cancelPending();
		}
		return applyCharacterwiseOperator(operator, range.start(), range.end());
	}

	private @Nullable TextObjectRange resolveWordTextObject(TextObjectScope scope, int repetitions) {
		String text = state.text();
		if (text.isEmpty()) {
			return null;
		}
		@Var int current = offset();
		if (current >= text.length()) {
			current = Math.max(0, text.length() - 1);
		}
		int initialClass = wordClass(text.charAt(current));
		@Var int start = current;
		while (start > 0 && wordClass(text.charAt(start - 1)) == initialClass && text.charAt(start - 1) != '\n') {
			start--;
		}
		@Var int end = current;
		while (end < text.length() && wordClass(text.charAt(end)) == initialClass && text.charAt(end) != '\n') {
			end++;
		}
		if (scope == TextObjectScope.AROUND) {
			@Var int trailing = end;
			while (trailing < text.length() && wordClass(text.charAt(trailing)) == 0 && text.charAt(trailing) != '\n') {
				trailing++;
			}
			if (trailing > end) {
				end = trailing;
			} else {
				@Var int leading = start;
				while (leading > 0 && wordClass(text.charAt(leading - 1)) == 0 && text.charAt(leading - 1) != '\n') {
					leading--;
				}
				start = leading;
			}
		}
		return new TextObjectRange(start, end);
	}

	private static @Nullable Character textObjectOpening(char key) {
		return switch (key) {
			case '(', ')', 'b' -> '(';
			case '[', ']' -> '[';
			case '{', '}', 'B' -> '{';
			case '\'', '"' -> key;
			default -> null;
		};
	}

	private Result awaitCharacter(Motion motion) {
		awaitingCharacterMotion = motion;
		return Result.HANDLED;
	}

	private Result completeCharacterMotion(String target) {
		Motion motion = awaitingCharacterMotion;
		awaitingCharacterMotion = null;
		if (motion == null) {
			return Result.HANDLED;
		}
		int repetitions = pendingOperator == null
				? consumeCount()
				: multipliedCount(operatorCount, consumeCount());
		MotionResult result = resolveMotion(motion, repetitions, null, target);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		lastSearch = new CharacterSearch(motion, target);
		if (pendingOperator != null) {
			return applyOperator(Objects.requireNonNull(pendingOperator), result);
		}
		Result res = moveTo(result);
		preferredColumn = state.cursorCol();
		return res;
	}

	private Result repeatLastSearch(boolean reverse) {
		CharacterSearch search = lastSearch;
		if (search == null) {
			clearPending();
			return Result.HANDLED;
		}
		Motion motion = reverse ? reverseSearchMotion(search.motion()) : search.motion();
		@Var int repetitions = pendingOperator == null
				? consumeCount()
				: multipliedCount(operatorCount, consumeCount());
		if (!reverse && isAdjacentSearchTarget(motion, search.target())) {
			repetitions = Math.min(MAX_COUNT, repetitions + 1);
		}
		MotionResult result = resolveMotion(motion, repetitions, null, search.target());
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		if (pendingOperator != null) {
			return applyOperator(Objects.requireNonNull(pendingOperator), result);
		}
		Result res = moveTo(result);
		preferredColumn = state.cursorCol();
		return res;
	}

	private Result executeStandaloneMotion(Motion motion, int repetitions, @Nullable Integer absoluteLine) {
		MotionResult result = resolveMotion(motion, repetitions, absoluteLine, null);
		if (result == null) {
			clearPending();
			if (motion == Motion.LINE_END) {
				preferredColumn = Integer.MAX_VALUE;
			}
			return Result.HANDLED;
		}
		Result res = moveTo(result);
		if (motion == Motion.LINE_END) {
			preferredColumn = Integer.MAX_VALUE;
		} else if (motion != Motion.LINE_DOWN && motion != Motion.LINE_UP) {
			preferredColumn = state.cursorCol();
		}
		return res;
	}

	private Result executeStandaloneDocumentMotion(Motion motion) {
		Integer target = consumeOptionalCount();
		return executeStandaloneMotion(motion, 1, target);
	}

	private Result executeOperatorMotion(Motion motion) {
		Operator operator = Objects.requireNonNull(pendingOperator);
		int repetitions = multipliedCount(operatorCount, consumeCount());
		MotionResult result = resolveMotion(motion, repetitions, null, null);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		return applyOperator(operator, result);
	}

	private Result executeOperatorDocumentMotion(Motion motion) {
		Operator operator = Objects.requireNonNull(pendingOperator);
		Integer motionLine = consumeOptionalCount();
		Integer targetLine = motionLine != null ? motionLine : operatorCount > 1 ? operatorCount : null;
		MotionResult result = resolveMotion(motion, 1, targetLine, null);
		if (result == null) {
			clearPending();
			return Result.HANDLED;
		}
		return applyOperator(operator, result);
	}

	private Result moveTo(MotionResult motion) {
		setPositionFromOffset(motion.targetOffset());
		normalizeNormalCursor();
		clearPending();
		return Result.HANDLED;
	}

	private @Nullable MotionResult resolveMotion(Motion motion, int repetitions,
												 @Nullable Integer absoluteLine, @Nullable String searchTarget) {
		int current = offset();
		int lineStart = current - state.cursorCol();
		int lineEnd = lineStart + state.getLine(state.cursorRow()).length();
		return switch (motion) {
			case LEFT -> {
				@Var int target = current;
				for (int i = 0; i < repetitions && target > lineStart; i++) {
					target = previousOffset(target);
				}
				yield target == current ? null : new MotionResult(target, false, false);
			}
			case RIGHT -> {
				@Var int target = current;
				for (int i = 0; i < repetitions && target < lineEnd; i++) {
					target = nextOffset(target);
				}
				yield target == current ? null : new MotionResult(target, false, false);
			}
			case WORD_FORWARD -> {
				int target = wordTarget(current, 1, repetitions);
				yield target == current ? null : new MotionResult(target, false, false);
			}
			case WORD_BACKWARD -> {
				int target = wordTarget(current, -1, repetitions);
				yield target == current ? null : new MotionResult(target, false, false);
			}
			case WORD_END -> {
				int target = wordEndTarget(current, repetitions);
				yield new MotionResult(target, false, true);
			}
			case LINE_START -> current == lineStart ? null : new MotionResult(lineStart, false, false);
			case LINE_END -> resolveLineEndMotion(repetitions);
			case LINE_DOWN -> resolveVerticalMotion(repetitions, 1);
			case LINE_UP -> resolveVerticalMotion(repetitions, -1);
			case FIRST_LINE -> resolveDocumentMotion(absoluteLine == null ? 1 : absoluteLine);
			case LAST_LINE -> resolveDocumentMotion(absoluteLine == null ? state.lineCount() : absoluteLine);
			case MATCHING_DELIMITER -> resolveMatchingDelimiter();
			case FIND_FORWARD, FIND_BACKWARD, TILL_FORWARD, TILL_BACKWARD ->
					resolveCharacterSearch(motion, repetitions, Objects.requireNonNull(searchTarget));
		};
	}

	private @Nullable MotionResult resolveMatchingDelimiter() {
		String text = state.text();
		DelimiterMap delimiters = scanDelimiters(text);
		int current = offset();
		int lineEnd = current - state.cursorCol() + state.getLine(state.cursorRow()).length();
		@Var int delimiterOffset = current;
		if (!isDelimiter(delimiters, delimiterOffset)) {
			delimiterOffset = -1;
			for (int i = current; i < lineEnd; i++) {
				if (isDelimiter(delimiters, i)) {
					delimiterOffset = i;
					break;
				}
			}
		}
		if (delimiterOffset < 0 || delimiters.pairs()[delimiterOffset] < 0) {
			return null;
		}
		return new MotionResult(delimiters.pairs()[delimiterOffset], false, true);
	}

	private static DelimiterMap scanDelimiters(String text) {
		int[] pairs = new int[text.length()];
		Arrays.fill(pairs, -1);
		boolean[] structural = new boolean[text.length()];
		int[] doubleQuotePairs = new int[text.length()];
		Arrays.fill(doubleQuotePairs, -1);
		Deque<Delimiter> stack = new ArrayDeque<>();
		Deque<Integer> stringStack = new ArrayDeque<>();
		@Var boolean inString = false;
		@Var boolean inComment = false;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (inComment) {
				if (ch == '\n' || ch == '\r') {
					inComment = false;
				}
				continue;
			}
			if (inString) {
				if (ch == '\\' && i + 1 < text.length()) {
					if (text.charAt(i + 1) == '(') {
						i++;
						structural[i] = true;
						stack.push(new Delimiter('(', i, true));
						inString = false;
					} else {
						i++;
					}
				} else if (ch == '"') {
					int opening = stringStack.pop();
					doubleQuotePairs[opening] = i;
					doubleQuotePairs[i] = opening;
					inString = false;
				}
				continue;
			}
			if (ch == '#') {
				inComment = true;
				continue;
			}
			if (ch == '"') {
				stringStack.push(i);
				inString = true;
				continue;
			}
			if (isOpeningDelimiter(ch)) {
				structural[i] = true;
				stack.push(new Delimiter(ch, i, false));
				continue;
			}
			if (!isClosingDelimiter(ch)) {
				continue;
			}
			structural[i] = true;
			if (stack.isEmpty()) {
				continue;
			}
			if (!isPair(stack.peek().value(), ch)) {
				stack.clear();
				continue;
			}
			Delimiter opening = stack.pop();
			pairs[opening.offset()] = i;
			pairs[i] = opening.offset();
			if (opening.interpolation()) {
				inString = true;
			}
		}
		return new DelimiterMap(pairs, structural, doubleQuotePairs);
	}

	private @Nullable TextObjectRange resolveTextObject(
			char requestedOpening, TextObjectScope scope, int repetitions) {
		if (requestedOpening == '"' || requestedOpening == '\'') {
			return resolveQuoteTextObject(requestedOpening, scope, repetitions);
		}
		String text = state.text();
		DelimiterMap delimiters = scanDelimiters(text);
		int current = offset();
		@Var int opening = innermostEnclosingDelimiter(text, delimiters, requestedOpening, current);
		if (opening < 0) {
			int lineEnd = current - state.cursorCol() + state.getLine(state.cursorRow()).length();
			opening = nextPairedOpening(text, delimiters, requestedOpening, current, lineEnd);
		}
		if (opening < 0) {
			return null;
		}
		@Var int closing = delimiters.pairs()[opening];
		for (int i = 1; i < repetitions; i++) {
			opening = nextEnclosingDelimiter(text, delimiters, requestedOpening, opening, closing);
			if (opening < 0) {
				return null;
			}
			closing = delimiters.pairs()[opening];
		}
		return scope == TextObjectScope.INNER
				? new TextObjectRange(opening + 1, closing)
				: new TextObjectRange(opening, closing + 1);
	}

	private @Nullable TextObjectRange resolveQuoteTextObject(
			char quote, TextObjectScope scope, int repetitions) {
		String text = state.text();
		int current = offset();
		int lineStart = current - state.cursorCol();
		int lineEnd = lineStart + state.getLine(state.cursorRow()).length();
		int[] pairs = quote == '"'
				? scanDelimiters(text).doubleQuotePairs()
				: scanSingleQuotePairs(text, lineStart, lineEnd);
		@Var int opening = innermostEnclosingQuote(text, pairs, quote, current, lineStart, lineEnd);
		if (opening < 0) {
			opening = nextPairedQuote(text, pairs, quote, current, lineEnd);
		}
		if (opening < 0) {
			return null;
		}
		int closing = pairs[opening];
		if (scope == TextObjectScope.INNER) {
			return repetitions == 2
					? new TextObjectRange(opening, closing + 1)
					: new TextObjectRange(opening + 1, closing);
		}

		@Var int start = opening;
		@Var int end = closing + 1;
		if (current < opening && containsOnlyHorizontalWhitespace(text, current, opening)) {
			while (start > lineStart && isHorizontalWhitespace(text.charAt(start - 1))) {
				start--;
			}
			return new TextObjectRange(start, end);
		}
		while (end < lineEnd && isHorizontalWhitespace(text.charAt(end))) {
			end++;
		}
		if (end == closing + 1) {
			while (start > lineStart && isHorizontalWhitespace(text.charAt(start - 1))) {
				start--;
			}
		}
		return new TextObjectRange(start, end);
	}

	private static int[] scanSingleQuotePairs(String text, int lineStart, int lineEnd) {
		int[] pairs = new int[text.length()];
		Arrays.fill(pairs, -1);
		@Var int opening = -1;
		for (int i = lineStart; i < lineEnd; i++) {
			if (text.charAt(i) != '\'' || isBackslashEscaped(text, i, lineStart)) {
				continue;
			}
			if (opening < 0) {
				opening = i;
			} else {
				pairs[opening] = i;
				pairs[i] = opening;
				opening = -1;
			}
		}
		return pairs;
	}

	private static boolean isBackslashEscaped(String text, int offset, int lineStart) {
		@Var int backslashes = 0;
		for (int i = offset - 1; i >= lineStart && text.charAt(i) == '\\'; i--) {
			backslashes++;
		}
		return backslashes % 2 != 0;
	}

	private static int innermostEnclosingQuote(
			String text, int[] pairs, char quote, int cursor, int lineStart, int lineEnd) {
		@Var int result = -1;
		for (int i = lineStart; i < lineEnd; i++) {
			int closing = pairs[i];
			if (text.charAt(i) == quote && closing > i && closing < lineEnd && i <= cursor && cursor <= closing) {
				result = i;
			}
		}
		return result;
	}

	private static int nextPairedQuote(String text, int[] pairs, char quote, int start, int lineEnd) {
		for (int i = start; i < lineEnd; i++) {
			if (text.charAt(i) == quote && pairs[i] > i && pairs[i] < lineEnd) {
				return i;
			}
		}
		return -1;
	}

	private static boolean containsOnlyHorizontalWhitespace(String text, int start, int end) {
		for (int i = start; i < end; i++) {
			if (!isHorizontalWhitespace(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isHorizontalWhitespace(char ch) {
		return ch != '\n' && ch != '\r' && Character.isWhitespace(ch);
	}

	private static int innermostEnclosingDelimiter(
			String text, DelimiterMap delimiters, char requestedOpening, int cursor) {
		@Var int result = -1;
		for (int i = 0; i < text.length(); i++) {
			int closing = delimiters.pairs()[i];
			if (text.charAt(i) == requestedOpening && closing > i && i <= cursor && cursor <= closing) {
				result = i;
			}
		}
		return result;
	}

	private static int nextPairedOpening(
			String text, DelimiterMap delimiters, char requestedOpening, int start, int end) {
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == requestedOpening && delimiters.pairs()[i] > i) {
				return i;
			}
		}
		return -1;
	}

	private static int nextEnclosingDelimiter(
			String text, DelimiterMap delimiters, char requestedOpening, int opening, int closing) {
		for (int i = opening - 1; i >= 0; i--) {
			if (text.charAt(i) == requestedOpening && delimiters.pairs()[i] > closing) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isDelimiter(DelimiterMap delimiters, int offset) {
		return offset >= 0 && offset < delimiters.structural().length && delimiters.structural()[offset];
	}

	private static boolean isOpeningDelimiter(char ch) {
		return ch == '(' || ch == '{' || ch == '[';
	}

	private static boolean isClosingDelimiter(char ch) {
		return ch == ')' || ch == '}' || ch == ']';
	}

	private static boolean isPair(char opening, char closing) {
		return (opening == '(' && closing == ')')
				|| (opening == '{' && closing == '}')
				|| (opening == '[' && closing == ']');
	}

	private @Nullable MotionResult resolveVerticalMotion(int repetitions, int direction) {
		int currentRow = state.cursorRow();
		int targetRow = Math.max(0, Math.min(state.lineCount() - 1, currentRow + direction * repetitions));
		if (targetRow == currentRow) {
			return null;
		}
		String targetLine = state.getLine(targetRow);
		int maxCol = Math.max(0, targetLine.length() - 1);
		int col = Math.min(preferredColumn, maxCol);
		return new MotionResult(offsetForPosition(targetRow, col), true, true);
	}

	private @Nullable MotionResult resolveLineEndMotion(int repetitions) {
		int targetRow = Math.min(state.lineCount() - 1, state.cursorRow() + repetitions - 1);
		String targetLine = state.getLine(targetRow);
		int targetLineStart = offsetForPosition(targetRow, 0);
		if (targetLine.isEmpty()) {
			return targetRow == state.cursorRow() ? null : new MotionResult(targetLineStart, false, false);
		}
		int target = previousOffset(targetLineStart + targetLine.length());
		return new MotionResult(target, false, true);
	}

	private MotionResult resolveDocumentMotion(int oneBasedLine) {
		int targetRow = Math.max(0, Math.min(state.lineCount() - 1, oneBasedLine - 1));
		String line = state.getLine(targetRow);
		@Var int col = 0;
		while (col < line.length() && Character.isWhitespace(line.charAt(col))) {
			col++;
		}
		return new MotionResult(offsetForPosition(targetRow, col), true, true);
	}

	private @Nullable MotionResult resolveCharacterSearch(Motion motion, int repetitions, String targetText) {
		String line = state.getLine(state.cursorRow());
		int lineStart = offset() - state.cursorCol();
		@Var int found;
		if (motion == Motion.FIND_FORWARD || motion == Motion.TILL_FORWARD) {
			@Var int from = nextOffset(offset()) - lineStart;
			found = -1;
			for (int i = 0; i < repetitions; i++) {
				found = line.indexOf(targetText, from);
				if (found < 0) {
					return null;
				}
				from = Math.min(line.length(), found + targetText.length());
			}
			int targetCol = motion == Motion.TILL_FORWARD ? previousOffset(lineStart + found) - lineStart : found;
			if (targetCol == state.cursorCol()) {
				return null;
			}
			return new MotionResult(offsetForPosition(state.cursorRow(), targetCol), false, true);
		}

		@Var int from = previousOffset(offset()) - lineStart;
		found = -1;
		for (int i = 0; i < repetitions; i++) {
			found = line.lastIndexOf(targetText, from);
			if (found < 0) {
				return null;
			}
			from = found - 1;
		}
		int targetCol = motion == Motion.TILL_BACKWARD ? found + targetText.length() : found;
		if (targetCol == state.cursorCol()) {
			return null;
		}
		return new MotionResult(offsetForPosition(state.cursorRow(), targetCol), false, true);
	}

	private Result applyOperator(Operator operator, MotionResult motion) {
		if (motion.linewise()) {
			int targetRow = positionForOffset(state.text(), motion.targetOffset())[0];
			return applyLinewiseOperator(operator, 1, targetRow);
		}
		int current = offset();
		int start;
		int end;
		if (motion.targetOffset() >= current) {
			start = current;
			end = motion.inclusive() ? nextOffset(motion.targetOffset()) : motion.targetOffset();
		} else {
			start = motion.targetOffset();
			end = motion.inclusive() ? nextOffset(current) : current;
		}
		if (start == end) {
			clearPending();
			return Result.HANDLED;
		}
		return applyCharacterwiseOperator(operator, start, end);
	}

	private Result applyCharacterwiseOperator(Operator operator, int start, int end) {
		String text = state.text();
		String selected = text.substring(start, end);
		register = new Register(selected, false);
		if (operator == Operator.YANK) {
			clearPending();
			return Result.HANDLED;
		}
		Snapshot before = snapshot();
		String newText = text.substring(0, start) + text.substring(end);
		pushUndo(before);
		int[] cursor = positionForOffset(newText, Math.min(start, newText.length()));
		setTextAndPosition(newText, cursor[0], cursor[1]);
		if (operator == Operator.CHANGE) {
			beginInsert(true);
		} else {
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			clearPending();
		}
		return Result.CHANGED;
	}

	private Result applyLinewiseOperator(Operator operator, int lineCount, int targetRow) {
		List<String> lines = lines();
		int currentRow = state.cursorRow();
		int start = Math.min(currentRow, targetRow);
		@Var int end = Math.max(currentRow, targetRow) + lineCount;
		end = Math.min(lines.size(), end);
		register = new Register(String.join("\n", lines.subList(start, end)), true);
		if (operator == Operator.YANK) {
			clearPending();
			return Result.HANDLED;
		}
		Snapshot before = snapshot();
		lines.subList(start, end).clear();
		if (operator == Operator.CHANGE || lines.isEmpty()) {
			lines.add(Math.min(start, lines.size()), "");
		}
		int row = Math.min(start, lines.size() - 1);
		pushUndo(before);
		setTextAndPosition(String.join("\n", lines), row, 0);
		if (operator == Operator.CHANGE) {
			beginInsert(true);
		} else {
			preferredColumn = 0;
			clearPending();
		}
		return Result.CHANGED;
	}

	private int wordTarget(int start, int direction, int repetitions) {
		String text = state.text();
		if (text.isEmpty()) {
			return start;
		}
		@Var int target = start;
		for (int n = 0; n < repetitions; n++) {
			if (direction > 0) {
				if (target < text.length()) {
					int currentClass = wordClass(text.charAt(target));
					while (target < text.length() && wordClass(text.charAt(target)) == currentClass) {
						target++;
					}
				}
				while (target < text.length() && wordClass(text.charAt(target)) == 0) {
					target++;
				}
			} else {
				target = Math.max(0, target - 1);
				while (target > 0 && wordClass(text.charAt(target)) == 0) {
					target--;
				}
				int currentClass = wordClass(text.charAt(target));
				while (target > 0 && wordClass(text.charAt(target - 1)) == currentClass) {
					target--;
				}
			}
		}
		return Math.min(target, text.length());
	}

	private int wordEndTarget(int start, int repetitions) {
		String text = state.text();
		if (text.isEmpty()) {
			return start;
		}
		@Var int target = start;
		for (int n = 0; n < repetitions; n++) {
			if (target < text.length() - 1) {
				target++;
			}
			while (target < text.length() && wordClass(text.charAt(target)) == 0) {
				target++;
			}
			int currentClass = target < text.length() ? wordClass(text.charAt(target)) : 0;
			while (target + 1 < text.length() && wordClass(text.charAt(target + 1)) == currentClass) {
				target++;
			}
		}
		return Math.min(target, Math.max(0, text.length() - 1));
	}

	private boolean isOnWord() {
		String text = state.text();
		int current = offset();
		return current < text.length() && wordClass(text.charAt(current)) != 0;
	}

	private boolean isAdjacentSearchTarget(Motion motion, String target) {
		String line = state.getLine(state.cursorRow());
		if (motion == Motion.TILL_FORWARD) {
			int adjacent = nextOffset(offset()) - (offset() - state.cursorCol());
			return adjacent <= line.length() && line.startsWith(target, adjacent);
		}
		if (motion == Motion.TILL_BACKWARD) {
			int end = state.cursorCol();
			return end >= target.length() && line.regionMatches(end - target.length(), target, 0, target.length());
		}
		return false;
	}

	private static Motion reverseSearchMotion(Motion motion) {
		return switch (motion) {
			case FIND_FORWARD -> Motion.FIND_BACKWARD;
			case FIND_BACKWARD -> Motion.FIND_FORWARD;
			case TILL_FORWARD -> Motion.TILL_BACKWARD;
			case TILL_BACKWARD -> Motion.TILL_FORWARD;
			default -> throw new IllegalArgumentException("not a character-search motion: " + motion);
		};
	}

	private static char operatorKey(Operator operator) {
		return switch (operator) {
			case DELETE -> 'd';
			case CHANGE -> 'c';
			case YANK -> 'y';
		};
	}

	private static int multipliedCount(int first, int second) {
		return (int) Math.min((long) first * second, MAX_COUNT);
	}

	private Result enterCommandMode() {
		clearPending();
		mode = Mode.COMMAND;
		command.setLength(0);
		commandCursor = 0;
		return Result.HANDLED;
	}

	private Result enterSearchMode() {
		clearPending();
		mode = Mode.SEARCH;
		searchInput.setLength(0);
		searchInputCursor = 0;
		searchStart = snapshot();
		previewSearchMatches = List.of();
		previewSearchMatch = -1;
		searchError = null;
		return Result.HANDLED;
	}

	private void updateSearchPreview() {
		Snapshot start = Objects.requireNonNull(searchStart);
		if (searchInput.length() == 0) {
			previewSearchMatches = List.of();
			previewSearchMatch = -1;
			searchError = null;
			setPosition(start.row(), start.col());
			return;
		}
		try {
			previewSearchMatches = findMatches(searchInput.toString());
			searchError = null;
			previewSearchMatch = nextMatchAfter(previewSearchMatches, offsetForSnapshot(start));
			if (previewSearchMatch >= 0) {
				setPositionFromOffset(previewSearchMatches.get(previewSearchMatch).start());
			} else {
				setPosition(start.row(), start.col());
			}
		} catch (PatternSyntaxException e) {
			previewSearchMatches = List.of();
			previewSearchMatch = -1;
			searchError = e.getDescription();
			setPosition(start.row(), start.col());
		}
	}

	private List<SearchMatch> findMatches(String expression) {
		Matcher matcher = Pattern.compile(expression).matcher(state.text());
		List<SearchMatch> matches = new ArrayList<>();
		while (matcher.find()) {
			matches.add(new SearchMatch(matcher.start(), matcher.end()));
		}
		return List.copyOf(matches);
	}

	private Result navigateQuerySearch(boolean forward, int repetitions) {
		if (searchPattern == null) {
			clearPending();
			return Result.HANDLED;
		}
		recomputeCommittedSearch();
		if (searchMatches.isEmpty()) {
			message = "Pattern not found: " + searchPattern;
			return Result.HANDLED;
		}
		@Var int currentOffset = offset();
		@Var int index = matchAtOffset(searchMatches, currentOffset);
		for (int i = 0; i < repetitions; i++) {
			if (forward) {
				index = index >= 0 ? (index + 1) % searchMatches.size() : nextMatchAfter(searchMatches, currentOffset);
			} else {
				index = index >= 0
						? (index - 1 + searchMatches.size()) % searchMatches.size()
						: previousMatchBefore(searchMatches, currentOffset);
			}
			currentOffset = searchMatches.get(index).start();
		}
		activeSearchMatch = index;
		setPositionFromOffset(searchMatches.get(index).start());
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
		return Result.HANDLED;
	}

	private void recomputeCommittedSearch() {
		if (searchPattern == null) {
			return;
		}
		searchMatches = findMatches(searchPattern);
		activeSearchMatch = matchAtOffset(searchMatches, offset());
		if (activeSearchMatch < 0 && !searchMatches.isEmpty()) {
			activeSearchMatch = nextMatchAtOrAfter(searchMatches, offset());
		}
	}

	private void clearQuerySearch() {
		searchPattern = null;
		searchMatches = List.of();
		activeSearchMatch = -1;
		searchInput.setLength(0);
		previewSearchMatches = List.of();
		previewSearchMatch = -1;
		searchError = null;
		searchStart = null;
	}

	private void cancelSearchInput(boolean restoreCursor) {
		if (mode != Mode.SEARCH) {
			return;
		}
		Snapshot start = searchStart;
		if (restoreCursor && start != null) {
			setPosition(start.row(), start.col());
			preferredColumn = start.preferredColumn();
		}
		finishSearchInput();
	}

	private void finishSearchInput() {
		mode = Mode.NORMAL;
		preferredColumn = state.cursorCol();
		searchInput.setLength(0);
		searchInputCursor = 0;
		previewSearchMatches = List.of();
		previewSearchMatch = -1;
		searchError = null;
		searchStart = null;
	}

	private String searchStatusText() {
		String prefix = "/" + searchInput;
		if (searchError != null) {
			return prefix + " [Invalid regex: " + searchError + "]";
		}
		return prefix + searchCount(previewSearchMatches, previewSearchMatch);
	}

	private static String searchSummary(String expression, List<SearchMatch> matches, int active) {
		return "/" + expression + searchCount(matches, active);
	}

	private static String searchCount(List<SearchMatch> matches, int active) {
		return matches.isEmpty() ? " [0/0]" : " [" + (active + 1) + "/" + matches.size() + "]";
	}

	private static int nextMatchAfter(List<SearchMatch> matches, int offset) {
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).start() > offset) {
				return i;
			}
		}
		return matches.isEmpty() ? -1 : 0;
	}

	private static int nextMatchAtOrAfter(List<SearchMatch> matches, int offset) {
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).start() >= offset) {
				return i;
			}
		}
		return matches.isEmpty() ? -1 : 0;
	}

	private static int previousMatchBefore(List<SearchMatch> matches, int offset) {
		for (int i = matches.size() - 1; i >= 0; i--) {
			if (matches.get(i).start() < offset) {
				return i;
			}
		}
		return matches.size() - 1;
	}

	private static int matchAtOffset(List<SearchMatch> matches, int offset) {
		for (int i = 0; i < matches.size(); i++) {
			if (matches.get(i).start() == offset) {
				return i;
			}
		}
		return -1;
	}

	private int offsetForSnapshot(Snapshot snapshot) {
		return offsetForPosition(snapshot.row(), snapshot.col());
	}

	private Result enterInsertAtCursor() {
		beginInsert(false);
		return Result.HANDLED;
	}

	private Result enterInsertAfterCursor() {
		if (state.cursorCol() < state.getLine(state.cursorRow()).length()) {
			state.moveCursorRight();
		}
		beginInsert(false);
		return Result.HANDLED;
	}

	private Result enterInsertAtLineStart() {
		state.moveCursorToLineStart();
		beginInsert(false);
		return Result.HANDLED;
	}

	private Result enterInsertAtLineEnd() {
		state.moveCursorToLineEnd();
		beginInsert(false);
		return Result.HANDLED;
	}

	private Result openLine(boolean after) {
		pushUndo(snapshot());
		List<String> lines = lines();
		int row = state.cursorRow() + (after ? 1 : 0);
		lines.add(row, "");
		setTextAndPosition(String.join("\n", lines), row, 0);
		beginInsert(true);
		return Result.CHANGED;
	}

	private static int wordClass(char ch) {
		if (Character.isWhitespace(ch)) {
			return 0;
		}
		return Character.isLetterOrDigit(ch) || ch == '_' ? 1 : 2;
	}

	private Result deleteCharacters(boolean backward, int repetitions) {
		Snapshot before = snapshot();
		StringBuilder removed = new StringBuilder();
		for (int i = 0; i < repetitions; i++) {
			String line = state.getLine(state.cursorRow());
			if (backward) {
				if (state.cursorCol() == 0) {
					break;
				}
				int oldCol = state.cursorCol();
				state.deleteBackward();
				removed.insert(0, line.substring(state.cursorCol(), oldCol));
			} else {
				if (state.cursorCol() >= line.length()) {
					break;
				}
				int oldLength = line.length();
				int oldCol = state.cursorCol();
				state.deleteForward();
				int removedLength = oldLength - state.getLine(state.cursorRow()).length();
				removed.append(line, oldCol, oldCol + removedLength);
			}
		}
		if (removed.length() == 0) {
			return Result.HANDLED;
		}
		pushUndo(before);
		register = new Register(removed.toString(), false);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		return Result.CHANGED;
	}

	private Result startReplace() {
		replaceCount = consumeCount();
		awaitingReplace = true;
		return Result.HANDLED;
	}

	private Result completeReplace(String replacement) {
		int repetitions = replaceCount;
		clearPending();

		String line = state.getLine(state.cursorRow());
		if (line.isEmpty() || state.cursorCol() >= line.length()) {
			return Result.HANDLED;
		}

		TextAreaState temp = new TextAreaState(line);
		temp.moveCursorToStart();
		while (temp.cursorCol() < state.cursorCol()) {
			temp.moveCursorRight();
		}

		int initialLength = temp.getLine(0).length();
		for (int i = 0; i < repetitions; i++) {
			if (temp.cursorCol() >= temp.getLine(0).length()) {
				return Result.HANDLED;
			}
			temp.deleteForward();
		}
		int charactersDeletedLength = initialLength - temp.getLine(0).length();

		Snapshot before = snapshot();
		int currentRow = state.cursorRow();
		int currentCol = state.cursorCol();
		List<String> lines = lines();

		if (replacement.equals("\n")) {
			String beforeBreak = line.substring(0, currentCol);
			String afterBreak = line.substring(currentCol + charactersDeletedLength);
			lines.set(currentRow, beforeBreak);
			lines.add(currentRow + 1, afterBreak);
			pushUndo(before);
			setTextAndPosition(String.join("\n", lines), currentRow + 1, 0);
		} else {
			String inserted = replacement.repeat(repetitions);
			String newLine = line.substring(0, currentCol) + inserted + line.substring(currentCol + charactersDeletedLength);
			lines.set(currentRow, newLine);
			pushUndo(before);
			int targetCol = currentCol + (repetitions - 1) * replacement.length();
			setTextAndPosition(String.join("\n", lines), currentRow, targetCol);
		}
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		return Result.CHANGED;
	}

	private Result deleteToLineEnd(boolean enterInsert) {
		Snapshot before = snapshot();
		String line = state.getLine(state.cursorRow());
		String removed = line.substring(state.cursorCol());
		while (state.cursorCol() < state.getLine(state.cursorRow()).length()) {
			state.deleteForward();
		}
		if (!removed.isEmpty()) {
			pushUndo(before);
			register = new Register(removed, false);
		}
		if (enterInsert) {
			beginInsert(!removed.isEmpty());
		} else {
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
		}
		clearPending();
		return removed.isEmpty() ? Result.HANDLED : Result.CHANGED;
	}

	private Result paste(boolean after, int repetitions) {
		if (register.text().isEmpty() && !register.linewise() && !register.blockwise()) {
			return Result.HANDLED;
		}
		Snapshot before = snapshot();
		if (register.blockwise()) {
			List<String> lines = lines();
			List<String> blockLines = Arrays.asList(register.text().split("\n", -1));
			int startRow = state.cursorRow();
			int col = state.cursorCol() + (after && !state.getLine(startRow).isEmpty() ? 1 : 0);
			while (lines.size() < startRow + blockLines.size()) {
				lines.add("");
			}
			for (int i = 0; i < blockLines.size(); i++) {
				int r = startRow + i;
				String line = lines.get(r);
				String blockPart = blockLines.get(i).repeat(repetitions);
				StringBuilder sb = new StringBuilder(line);
				while (sb.length() < col) {
					sb.append(' ');
				}
				sb.insert(col, blockPart);
				lines.set(r, sb.toString());
			}
			pushUndo(before);
			setTextAndPosition(String.join("\n", lines), startRow, col);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
			return Result.CHANGED;
		}
		if (register.linewise()) {
			List<String> lines = lines();
			List<String> pasted = Arrays.asList(register.text().split("\n", -1));
			int row = state.cursorRow() + (after ? 1 : 0);
			for (int i = 0; i < repetitions; i++) {
				lines.addAll(row + i * pasted.size(), pasted);
			}
			String firstPastedLine = lines.get(row);
			@Var int col = 0;
			while (col < firstPastedLine.length() && Character.isWhitespace(firstPastedLine.charAt(col))) {
				col++;
			}
			setTextAndPosition(String.join("\n", lines), row, col);
			normalizeNormalCursor();
			preferredColumn = state.cursorCol();
		} else {
			StringBuilder inserted = new StringBuilder();
			for (int i = 0; i < repetitions; i++) {
				inserted.append(register.text());
			}
			String text = state.text();
			@Var int insertionOffset = offset();
			if (after && insertionOffset < text.length() && text.charAt(insertionOffset) != '\n') {
				insertionOffset = Character.offsetByCodePoints(text, insertionOffset, 1);
			}
			String newText = text.substring(0, insertionOffset) + inserted + text.substring(insertionOffset);
			int cursorOffset = insertionOffset + inserted.length();
			int[] cursorPosition = positionForOffset(newText, cursorOffset);
			setTextAndPosition(newText, cursorPosition[0], cursorPosition[1]);
			state.moveCursorLeft();
		}
		pushUndo(before);
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		return Result.CHANGED;
	}

	private Result undo(int repetitions) {
		@Var boolean changed = false;
		for (int i = 0; i < repetitions && !undoStack.isEmpty(); i++) {
			restore(undoStack.removeLast());
			changed = true;
		}
		clearPending();
		return changed ? Result.CHANGED : Result.HANDLED;
	}

	private void beginInsert(boolean undoCaptured) {
		clearPending();
		mode = Mode.INSERT;
		insertUndoCaptured = undoCaptured;
	}

	private void leaveInsertMode() {
		mode = Mode.NORMAL;
		replaceHistory.clear();
		if (blockInsertContext != null) {
			finishBlockInsert();
		}
		if (state.cursorCol() > 0) {
			state.moveCursorLeft();
		}
		normalizeNormalCursor();
		preferredColumn = state.cursorCol();
		clearPending();
	}

	private void finishBlockInsert() {
		BlockInsertContext ctx = blockInsertContext;
		blockInsertContext = null;
		String inserted = blockInsertedText.toString();
		blockInsertedText.setLength(0);
		if (inserted.isEmpty() || ctx == null) {
			return;
		}
		List<String> lines = lines();
		for (int r = ctx.startRow() + 1; r <= ctx.endRow() && r < lines.size(); r++) {
			String line = lines.get(r);
			StringBuilder sb = new StringBuilder(line);
			while (sb.length() < ctx.col()) {
				sb.append(' ');
			}
			sb.insert(ctx.col(), inserted);
			lines.set(r, sb.toString());
		}
		int targetRow = ctx.startRow();
		int targetCol = Math.max(0, ctx.col() + inserted.length() - 1);
		setTextAndPosition(String.join("\n", lines), targetRow, targetCol);
	}

	private Result enterReplaceMode() {
		consumeCount();
		beginReplace();
		return Result.HANDLED;
	}

	private void beginReplace() {
		clearPending();
		mode = Mode.REPLACE;
		replaceHistory.clear();
		insertUndoCaptured = false;
	}

	private void replaceCharacterInMode(String str) {
		String line = state.getLine(state.cursorRow());
		int row = state.cursorRow();
		int col = state.cursorCol();

		if (col >= line.length()) {
			state.insert(str);
			return;
		}

		TextAreaState temp = new TextAreaState(line);
		temp.moveCursorToStart();
		while (temp.cursorCol() < col) {
			temp.moveCursorRight();
		}
		int oldLen = temp.getLine(0).length();
		temp.deleteForward();
		int charLen = oldLen - temp.getLine(0).length();

		List<String> lines = lines();
		String newLine = line.substring(0, col) + str + line.substring(col + charLen);
		lines.set(row, newLine);
		setTextAndPosition(String.join("\n", lines), row, col + str.length());
	}

	private void captureInsertUndo() {
		if (!insertUndoCaptured) {
			pushUndo(snapshot());
			insertUndoCaptured = true;
		}
	}

	private void deletePreviousWord() {
		String line = state.getLine(state.cursorRow());
		@Var int col = state.cursorCol();
		while (col > 0 && Character.isWhitespace(line.charAt(col - 1))) {
			state.deleteBackward();
			col--;
		}
		while (col > 0 && !Character.isWhitespace(line.charAt(col - 1))) {
			state.deleteBackward();
			col--;
		}
	}

	private void deletePreviousWordInCommand() {
		if (commandCursor == 0) {
			return;
		}
		@Var int start = commandCursor;
		while (start > 0 && Character.isWhitespace(command.charAt(start - 1))) {
			start--;
		}
		while (start > 0 && !Character.isWhitespace(command.charAt(start - 1))) {
			start--;
		}
		command.delete(start, commandCursor);
		commandCursor = start;
	}

	private void deletePreviousWordInSearch() {
		if (searchInputCursor == 0) {
			return;
		}
		@Var int start = searchInputCursor;
		while (start > 0 && Character.isWhitespace(searchInput.charAt(start - 1))) {
			start--;
		}
		while (start > 0 && !Character.isWhitespace(searchInput.charAt(start - 1))) {
			start--;
		}
		searchInput.delete(start, searchInputCursor);
		searchInputCursor = start;
	}

	private void normalizeNormalCursor() {
		String line = state.getLine(state.cursorRow());
		if (!line.isEmpty() && state.cursorCol() >= line.length()) {
			state.moveCursorLeft();
		}
	}

	private Result changedSince(String before) {
		return before.equals(state.text()) ? Result.HANDLED : Result.CHANGED;
	}

	private Snapshot snapshot() {
		return new Snapshot(state.text(), state.cursorRow(), state.cursorCol(), preferredColumn);
	}

	private void pushUndo(Snapshot snapshot) {
		if (undoStack.size() == MAX_UNDO_ENTRIES) {
			undoStack.removeFirst();
		}
		undoStack.addLast(snapshot);
	}

	private void restore(Snapshot snapshot) {
		setTextAndPosition(snapshot.text(), snapshot.row(), snapshot.col());
		preferredColumn = snapshot.preferredColumn();
		if (mode == Mode.NORMAL) {
			normalizeNormalCursor();
		}
	}

	private List<String> lines() {
		return new ArrayList<>(Arrays.asList(state.text().split("\n", -1)));
	}

	private int offset() {
		@Var int result = state.cursorCol();
		for (int row = 0; row < state.cursorRow(); row++) {
			result += state.getLine(row).length() + 1;
		}
		return result;
	}

	private int offsetForPosition(int row, int col) {
		@Var int result = col;
		for (int currentRow = 0; currentRow < row; currentRow++) {
			result += state.getLine(currentRow).length() + 1;
		}
		return result;
	}

	private int nextOffset(int currentOffset) {
		int savedRow = state.cursorRow();
		int savedCol = state.cursorCol();
		setPositionFromOffset(currentOffset);
		state.moveCursorRight();
		int result = offset();
		setPosition(savedRow, savedCol);
		return result;
	}

	private int previousOffset(int currentOffset) {
		int savedRow = state.cursorRow();
		int savedCol = state.cursorCol();
		setPositionFromOffset(currentOffset);
		state.moveCursorLeft();
		int result = offset();
		setPosition(savedRow, savedCol);
		return result;
	}

	private void setPositionFromOffset(int offset) {
		int[] position = positionForOffset(state.text(), offset);
		setPosition(position[0], position[1]);
	}

	private static int[] positionForOffset(String text, int offset) {
		@Var int row = 0;
		@Var int lineStart = 0;
		for (int i = 0; i < Math.min(offset, text.length()); i++) {
			if (text.charAt(i) == '\n') {
				row++;
				lineStart = i + 1;
			}
		}
		return new int[] { row, Math.max(0, offset - lineStart) };
	}

	private void setTextAndPosition(String text, int row, int col) {
		state.setText(text);
		setPosition(row, col);
	}

	private void setPosition(int row, int col) {
		state.moveCursorToStart();
		for (int i = 0; i < row; i++) {
			state.moveCursorDown();
		}
		while (state.cursorCol() < col) {
			state.moveCursorRight();
		}
	}

	private int consumeCount() {
		Integer value = consumeOptionalCount();
		return value == null ? 1 : value;
	}

	private @Nullable Integer consumeOptionalCount() {
		if (count.length() == 0) {
			return null;
		}
		@Var int value;
		try {
			value = Math.min(Integer.parseInt(count.toString()), MAX_COUNT);
		} catch (NumberFormatException e) {
			value = MAX_COUNT;
		}
		count.setLength(0);
		return value;
	}

	private Result cancelPending() {
		clearPending();
		return Result.HANDLED;
	}

	private void clearPending() {
		count.setLength(0);
		pendingOperator = null;
		pendingTextObjectScope = null;
		operatorCount = 1;
		pendingG = false;
		awaitingCharacterMotion = null;
		awaitingReplace = false;
		replaceCount = 1;
	}

	private static boolean isEscape(KeyEvent key) {
		return key.isCancel() || key.code() == KeyCode.ESCAPE;
	}
}
