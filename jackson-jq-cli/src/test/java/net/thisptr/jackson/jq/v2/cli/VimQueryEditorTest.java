package net.thisptr.jackson.jq.v2.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.widgets.input.TextAreaState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class VimQueryEditorTest {
	@TempDir
	Path tempDir;

	@Test
	void groupsAnInsertSessionIntoOneUndoEntry() {
		TextAreaState state = new TextAreaState(".");
		VimQueryEditor editor = new VimQueryEditor(state);

		key(editor, 'A');
		text(editor, " | .name");
		escape(editor);

		assertThat(state.text()).isEqualTo(". | .name");
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);

		key(editor, 'u');
		assertThat(state.text()).isEqualTo(".");
	}

	@Test
	void supportsInsertAndOpenLineVariants() {
		TextAreaState state = new TextAreaState("bc");
		VimQueryEditor editor = new VimQueryEditor(state);

		key(editor, 'I');
		key(editor, 'a');
		escape(editor);
		key(editor, 'A');
		key(editor, 'd');
		escape(editor);
		text(editor, "0aX");
		escape(editor);
		assertThat(state.text()).isEqualTo("aXbcd");

		key(editor, 'O');
		text(editor, "before");
		escape(editor);
		key(editor, 'G');
		key(editor, 'o');
		text(editor, "after");
		escape(editor);
		assertThat(state.text()).isEqualTo("before\naXbcd\nafter");
	}

	@Test
	void supportsCountsAndWordAndDocumentMotions() {
		TextAreaState state = new TextAreaState("one two three\nfour five");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isZero();
		text(editor, "2w");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'e');
		assertThat(state.cursorCol()).isEqualTo(12);

		text(editor, "2gg");
		assertThat(state.cursorRow()).isEqualTo(1);
		key(editor, 'G');
		assertThat(state.cursorRow()).isEqualTo(1);
	}

	@Test
	void wordMotionsSeparateIdentifiersFromJqPunctuation() {
		TextAreaState state = new TextAreaState(".foo | map(.bar)");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggw");
		assertThat(state.cursorCol()).isEqualTo(1);
		key(editor, 'w');
		assertThat(state.cursorCol()).isEqualTo(5);
		key(editor, 'w');
		assertThat(state.cursorCol()).isEqualTo(7);
		key(editor, 'b');
		assertThat(state.cursorCol()).isEqualTo(5);
	}

	@Test
	void deletesYanksAndPastesWholeLines() {
		TextAreaState state = new TextAreaState("a\nb\nc");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggjyyp");
		assertThat(state.text()).isEqualTo("a\nb\nb\nc");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("a\nb\nc");

		text(editor, "2ddP");
		assertThat(state.text()).isEqualTo("b\nc\na");
	}

	@Test
	void supportsCharacterEditsAndChangeToLineEnd() {
		TextAreaState state = new TextAreaState("abc");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "02xP");
		assertThat(state.text()).isEqualTo("abc");

		text(editor, "0Cxy");
		escape(editor);
		assertThat(state.text()).isEqualTo("xy");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("abc");
	}

	@Test
	void supportsDeleteOperatorMotionsAndCounts() {
		TextAreaState state = new TextAreaState("one two three");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggdw");
		assertThat(state.text()).isEqualTo("two three");
		key(editor, 'u');
		text(editor, "wdb");
		assertThat(state.text()).isEqualTo("two three");
		key(editor, 'u');
		text(editor, "gglldh");
		assertThat(state.text()).isEqualTo("oe two three");

		TextAreaState countedState = new TextAreaState("one two three four five");
		VimQueryEditor countedEditor = new VimQueryEditor(countedState);
		text(countedEditor, "gg2d2w");
		assertThat(countedState.text()).isEqualTo("five");

		TextAreaState lineEndState = new TextAreaState("ab\ncd\nef");
		VimQueryEditor lineEndEditor = new VimQueryEditor(lineEndState);
		text(lineEndEditor, "ggd2$");
		assertThat(lineEndState.text()).isEqualTo("\nef");
	}

	@Test
	void supportsChangeAndYankOperatorMotions() {
		TextAreaState state = new TextAreaState("one two");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggcwX");
		escape(editor);
		assertThat(state.text()).isEqualTo("X two");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("one two");

		text(editor, "ggywP");
		assertThat(state.text()).isEqualTo("one one two");
	}

	@Test
	void supportsLinewiseOperatorMotions() {
		TextAreaState state = new TextAreaState("a\nb\nc\nd");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggdj");
		assertThat(state.text()).isEqualTo("c\nd");
		key(editor, 'u');
		text(editor, "2dd");
		assertThat(state.text()).isEqualTo("c\nd");
		key(editor, 'u');
		text(editor, "jdG");
		assertThat(state.text()).isEqualTo("a");
		key(editor, 'u');
		text(editor, "Gdgg");
		assertThat(state.text()).isEmpty();

		key(editor, 'u');
		text(editor, "ggccreplacement");
		escape(editor);
		assertThat(state.text()).isEqualTo("replacement\nb\nc\nd");
	}

	@Test
	void supportsFindAndTillOperators() {
		TextAreaState state = new TextAreaState("abc:def:ghi");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggdt:");
		assertThat(state.text()).isEqualTo(":def:ghi");
		key(editor, 'u');
		text(editor, "df:");
		assertThat(state.text()).isEqualTo("def:ghi");
		key(editor, 'u');
		text(editor, "d2t:");
		assertThat(state.text()).isEqualTo(":ghi");

		key(editor, 'u');
		text(editor, "$dT:");
		assertThat(state.text()).isEqualTo("abc:def:");
		key(editor, 'u');
		text(editor, "dF:");
		assertThat(state.text()).isEqualTo("abc:def");
	}

	@Test
	void jumpsBetweenNestedMatchingDelimiters() {
		TextAreaState state = new TextAreaState("prefix {\n  [value()]\n}");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg%");
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isZero();
		key(editor, '%');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(7);

		text(editor, "j0%");
		assertThat(state.cursorCol()).isEqualTo(10);
		key(editor, '%');
		assertThat(state.cursorCol()).isEqualTo(2);
		key(editor, 'l');
		key(editor, '%');
		assertThat(state.cursorCol()).isEqualTo(9);
	}

	@Test
	void delimiterMotionIgnoresStringsAndCommentsButMatchesInterpolation() {
		TextAreaState state = new TextAreaState("\"[ignored]\" # {ignored}\n{\"value \\(.[0])\"}");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg%");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isZero();

		text(editor, "j0%");
		assertThat(state.cursorCol()).isEqualTo(16);
		key(editor, '%');
		assertThat(state.cursorCol()).isZero();

		text(editor, "f\\%");
		assertThat(state.cursorCol()).isEqualTo(14);
		key(editor, '%');
		assertThat(state.cursorCol()).isEqualTo(9);

		TextAreaState escapedState = new TextAreaState("\"escaped \\\" [ignored] and \\\\(\" {ok}");
		VimQueryEditor escapedEditor = new VimQueryEditor(escapedState);
		text(escapedEditor, "gg%");
		assertThat(escapedState.cursorCol()).isEqualTo(escapedState.text().length() - 1);

		TextAreaState unicodeState = new TextAreaState("😀 [value]");
		VimQueryEditor unicodeEditor = new VimQueryEditor(unicodeState);
		text(unicodeEditor, "gg%");
		assertThat(unicodeState.cursorCol()).isEqualTo(unicodeState.text().indexOf(']'));
	}

	@Test
	void matchingDelimiterWorksAsAnInclusiveOperatorMotion() {
		TextAreaState deleteState = new TextAreaState("{abc} tail");
		VimQueryEditor deleteEditor = new VimQueryEditor(deleteState);
		text(deleteEditor, "ggd%");
		assertThat(deleteState.text()).isEqualTo(" tail");

		TextAreaState changeState = new TextAreaState("[abc] tail");
		VimQueryEditor changeEditor = new VimQueryEditor(changeState);
		text(changeEditor, "ggc%replacement");
		escape(changeEditor);
		assertThat(changeState.text()).isEqualTo("replacement tail");

		TextAreaState yankState = new TextAreaState("(abc) tail");
		VimQueryEditor yankEditor = new VimQueryEditor(yankState);
		text(yankEditor, "ggy%p");
		assertThat(yankState.text()).isEqualTo("((abc)abc) tail");
	}

	@Test
	void delimiterMotionRejectsMismatchesAndConsumesCounts() {
		TextAreaState state = new TextAreaState("([)] next");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg2%");
		assertThat(state.cursorCol()).isZero();
		key(editor, 'w');
		assertThat(state.cursorCol()).isEqualTo(5);

		text(editor, "ggd%x");
		assertThat(state.text()).isEqualTo("[)] next");
	}

	@Test
	void supportsInnerAndAroundDelimiterTextObjectsAndAliases() {
		assertTextObject("[abc]", "di[", "[]");
		assertTextObject("[abc]", "di]", "[]");
		assertTextObject("[abc]", "da[", "");
		assertTextObject("(abc)", "di(", "()");
		assertTextObject("(abc)", "di)", "()");
		assertTextObject("(abc)", "dib", "()");
		assertTextObject("(abc)", "dab", "");
		assertTextObject("{abc}", "di{", "{}");
		assertTextObject("{abc}", "di}", "{}");
		assertTextObject("{abc}", "diB", "{}");
		assertTextObject("{abc}", "daB", "");
		assertTextObject("{\n  [1]\n}", "di{", "{}");
	}

	@Test
	void changesAndYanksDelimiterTextObjects() {
		TextAreaState changeState = new TextAreaState("[abc] tail");
		VimQueryEditor changeEditor = new VimQueryEditor(changeState);
		text(changeEditor, "ggllci[X");
		escape(changeEditor);
		assertThat(changeState.text()).isEqualTo("[X] tail");
		key(changeEditor, 'u');
		assertThat(changeState.text()).isEqualTo("[abc] tail");

		TextAreaState yankState = new TextAreaState("[abc]");
		VimQueryEditor yankEditor = new VimQueryEditor(yankState);
		text(yankEditor, "ggllyi[0p");
		assertThat(yankState.text()).isEqualTo("[abcabc]");
	}

	@Test
	void supportsInnerAndAroundQuoteTextObjects() {
		assertTextObject("\"abc\"", "di\"", "\"\"");
		assertTextObject("'abc'", "di'", "''");
		assertTextObject("foo \"bar\" baz", "da\"", "foo baz");
		assertTextObject("foo 'bar'", "da'", "foo");
	}

	@Test
	void changesAndYanksQuoteTextObjects() {
		TextAreaState changeState = new TextAreaState("\"abc\" tail");
		VimQueryEditor changeEditor = new VimQueryEditor(changeState);
		text(changeEditor, "ggci\"X");
		escape(changeEditor);
		assertThat(changeState.text()).isEqualTo("\"X\" tail");

		TextAreaState yankState = new TextAreaState("'abc'");
		VimQueryEditor yankEditor = new VimQueryEditor(yankState);
		text(yankEditor, "ggyi'0p");
		assertThat(yankState.text()).isEqualTo("'abcabc'");
	}

	@Test
	void quoteTextObjectsHandleWhitespaceEscapesAndCounts() {
		TextAreaState leadingState = new TextAreaState("foo  \"bar\"   baz");
		VimQueryEditor leadingEditor = new VimQueryEditor(leadingState);
		text(leadingEditor, "ggllll");
		text(leadingEditor, "da\"");
		assertThat(leadingState.text()).isEqualTo("foo   baz");

		assertTextObject("\"a\\\"xb\"", "di\"", "\"\"");
		assertTextObject("'a\\'xb'", "di'", "''");
		assertTextObject("\"abc\"", "2di\"", "");
	}

	@Test
	void doubleQuoteTextObjectsFollowJqLexicalStructure() {
		assertTextObject("\"outer \\(\"inner x\") tail\"", "di\"", "\"outer \\(\"\") tail\"");
		assertTextObject("\"outer \\(.x) tail\"", "di\"", "\"\"");

		TextAreaState commentState = new TextAreaState("# \"ignored\"\n\"real\"");
		VimQueryEditor commentEditor = new VimQueryEditor(commentState);
		text(commentEditor, "ggdi\"");
		assertThat(commentState.text()).isEqualTo("# \"ignored\"\n\"real\"");
		text(commentEditor, "jdi\"");
		assertThat(commentState.text()).isEqualTo("# \"ignored\"\n\"\"");

		assertTextObject("# 'x'", "di'", "# ''");
	}

	@Test
	void emptyUnmatchedAndMultilineQuoteTextObjectsCancelWithoutEditing() {
		assertTextObject("\"\"", "di\"", "\"\"");
		assertTextObject("\"abc", "di\"", "\"abc");
		assertTextObject("\"abc\ndef\"", "di\"", "\"abc\ndef\"");
		assertTextObject("prefix\n\"abc\"", "di\"", "prefix\n\"abc\"");
	}

	@Test
	void expandsCountedTextObjectsToOuterPairs() {
		assertTextObject("[[[x]]]", "d2i[", "[[]]");
		assertTextObject("[[[x]]]", "2di[", "[[]]");
		assertTextObject("[[[x]]]", "d2a[", "[]");
		assertTextObject("[[[[x]]]]", "2d2i[", "[]");
		assertTextObject("[[x]]", "d3i[", "[[x]]");
	}

	@Test
	void findsTheNextTextObjectOnTheCurrentLine() {
		assertTextObject("prefix [abc]", "di[", "prefix []");
		assertTextObject("prefix\n[abc]", "di[", "prefix\n[abc]");
		assertTextObject("# [ignored]\n[real]", "di[", "# [ignored]\n[real]");
		assertTextObject("\"[ignored]\" [real]", "di[", "\"[ignored]\" []");

		TextAreaState interpolationState = new TextAreaState("\"value \\(.[0])\"");
		VimQueryEditor interpolationEditor = new VimQueryEditor(interpolationState);
		text(interpolationEditor, "ggdi(");
		assertThat(interpolationState.text()).isEqualTo("\"value \\()\"");
	}

	@Test
	void emptyAndInvalidTextObjectsCancelWithoutEditing() {
		TextAreaState emptyState = new TextAreaState("[]");
		VimQueryEditor emptyEditor = new VimQueryEditor(emptyState);
		text(emptyEditor, "ggci[");
		assertThat(emptyState.text()).isEqualTo("[]");
		assertThat(emptyEditor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
		assertTextObject("[abc", "di[", "[abc");

		TextAreaState invalidState = new TextAreaState("abc");
		VimQueryEditor invalidEditor = new VimQueryEditor(invalidState);
		text(invalidEditor, "ggdiqx");
		assertThat(invalidState.text()).isEqualTo("bc");
	}

	@Test
	void supportsChangeAndYankWithCharacterSearches() {
		TextAreaState state = new TextAreaState("ab:cd");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggyt:P");
		assertThat(state.text()).isEqualTo("abab:cd");
		key(editor, 'u');

		text(editor, "ggct:X");
		escape(editor);
		assertThat(state.text()).isEqualTo("X:cd");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("ab:cd");
	}

	@Test
	void repeatsAndReversesCharacterSearches() {
		TextAreaState state = new TextAreaState("ab:cd:ef:gh");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggt:");
		assertThat(state.cursorCol()).isEqualTo(1);
		key(editor, ';');
		assertThat(state.cursorCol()).isEqualTo(4);
		key(editor, ';');
		assertThat(state.cursorCol()).isEqualTo(7);
		key(editor, ',');
		assertThat(state.cursorCol()).isEqualTo(6);

		text(editor, "ggf:");
		assertThat(state.cursorCol()).isEqualTo(2);
		text(editor, "fz");
		assertThat(state.cursorCol()).isEqualTo(2);
		key(editor, ';');
		assertThat(state.cursorCol()).isEqualTo(5);

		text(editor, "ggd;");
		assertThat(state.text()).isEqualTo("cd:ef:gh");
	}

	@Test
	void characterSearchAcceptsSupplementaryUnicodeTargets() {
		TextAreaState state = new TextAreaState("ab😀cd😀ef");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggdt");
		editor.handleKey(KeyEvent.ofChar(0x1F600));
		assertThat(state.text()).isEqualTo("😀cd😀ef");
		key(editor, 'u');

		text(editor, "f");
		editor.handleKey(KeyEvent.ofChar(0x1F600));
		assertThat(state.cursorCol()).isEqualTo(2);
		key(editor, ';');
		assertThat(state.cursorCol()).isEqualTo(6);
	}

	@Test
	void cancelsOrIgnoresIncompleteAndMissingOperatorMotions() {
		TextAreaState state = new TextAreaState("abc:def");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggd");
		escape(editor);
		key(editor, 'x');
		assertThat(state.text()).isEqualTo("bc:def");
		key(editor, 'u');

		text(editor, "ggdtz");
		assertThat(state.text()).isEqualTo("abc:def");
		text(editor, "dQ");
		assertThat(state.text()).isEqualTo("abc:def");
	}

	@Test
	void treatsExtendedGraphemeAsOneCharacter() {
		TextAreaState state = new TextAreaState("a👨‍👩‍👧‍👦b");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lx");
		assertThat(state.text()).isEqualTo("ab");

		key(editor, 'P');
		assertThat(state.text()).isEqualTo("a👨‍👩‍👧‍👦b");
	}

	@Test
	void parsesQuitCommandAndReportsUnknownCommands() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		key(editor, ':');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.COMMAND);
		assertThat(editor.statusText()).isEqualTo(":");
		key(editor, 'q');
		VimQueryEditor.Result result = editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(result.submitRequested()).isTrue();
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);

		text(editor, ":nope");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(editor.statusText()).isEqualTo("Not an editor command: nope");
	}

	@Test
	void writesCurrentFileAndKeepsAssociationWhenWritingAnotherFile() throws Exception {
		Path current = tempDir.resolve("current.jq");
		Path other = tempDir.resolve("other.jq");
		Files.writeString(current, ".", StandardCharsets.UTF_8);
		TextAreaState state = new TextAreaState(".");
		VimQueryEditor editor = new VimQueryEditor(state, current);

		text(editor, "Aname");
		escape(editor);
		command(editor, "write " + other);
		assertThat(editor.currentFile()).isEqualTo(current);
		assertThat(Files.readString(other)).isEqualTo(".name\n");
		assertThat(Files.readString(current)).isEqualTo(".");
		command(editor, "e");
		assertThat(editor.statusText()).startsWith("Unsaved changes");

		command(editor, "w");
		assertThat(Files.readString(current)).isEqualTo(".name\n");
		command(editor, "w " + other);
		assertThat(editor.statusText()).startsWith("File already exists");
		command(editor, "w! " + other);
		assertThat(Files.readString(other)).isEqualTo(".name\n");
	}

	@Test
	void saveAsUsesEscapedPathAndRequiresForceToOverwrite() throws Exception {
		Path file = tempDir.resolve("query file.jq");
		Files.writeString(file, "old", StandardCharsets.UTF_8);
		TextAreaState state = new TextAreaState(".value");
		VimQueryEditor editor = new VimQueryEditor(state);
		String escaped = file.toString().replace(" ", "\\ ");

		command(editor, "saveas " + escaped);
		assertThat(editor.statusText()).startsWith("File already exists");
		assertThat(Files.readString(file)).isEqualTo("old");
		command(editor, "saveas! " + escaped);
		assertThat(editor.currentFile()).isEqualTo(file);
		assertThat(Files.readString(file)).isEqualTo(".value\n");

		text(editor, "A | .name");
		escape(editor);
		command(editor, "w");
		assertThat(Files.readString(file)).isEqualTo(".value | .name\n");
	}

	@Test
	void editChecksUnsavedChangesAndResetsUndoHistory() throws Exception {
		Path file = tempDir.resolve("query.jq");
		Files.writeString(file, ".first", StandardCharsets.UTF_8);
		TextAreaState state = new TextAreaState(".first");
		VimQueryEditor editor = new VimQueryEditor(state, file);

		text(editor, "A | .second");
		escape(editor);
		command(editor, "edit");
		assertThat(state.text()).isEqualTo(".first | .second");
		assertThat(editor.statusText()).startsWith("Unsaved changes");
		command(editor, "e!");
		assertThat(state.text()).isEqualTo(".first");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo(".first");

		Files.writeString(file, ".changed", StandardCharsets.UTF_8);
		command(editor, "e");
		assertThat(state.text()).isEqualTo(".changed");

		Path next = tempDir.resolve("next.jq");
		Files.writeString(next, ".next", StandardCharsets.UTF_8);
		command(editor, "edit " + next);
		assertThat(editor.currentFile()).isEqualTo(next);
		assertThat(state.text()).isEqualTo(".next");
		text(editor, "A | .value");
		escape(editor);
		command(editor, "w");
		assertThat(Files.readString(next)).isEqualTo(".next | .value\n");
		assertThat(Files.readString(file)).isEqualTo(".changed");
	}

	@Test
	void undoToSavedTextAllowsEditWithoutForce() throws Exception {
		Path file = tempDir.resolve("query.jq");
		Files.writeString(file, ".", StandardCharsets.UTF_8);
		TextAreaState state = new TextAreaState(".");
		VimQueryEditor editor = new VimQueryEditor(state, file);
		text(editor, "Aname");
		escape(editor);
		key(editor, 'u');
		command(editor, "e");
		assertThat(editor.statusText()).startsWith("Opened:");
	}

	@Test
	void readInsertsAfterCurrentLineAndCanBeUndone() throws Exception {
		Path file = tempDir.resolve("lines.jq");
		Files.writeString(file, "one\ntwo\n", StandardCharsets.UTF_8);
		TextAreaState state = new TextAreaState("top\nbottom");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg");
		command(editor, "read " + file);
		assertThat(state.text()).isEqualTo("top\none\ntwo\nbottom");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("top\nbottom");
		command(editor, "r! " + file);
		assertThat(editor.statusText()).contains("not supported");
		assertThat(state.text()).isEqualTo("top\nbottom");
	}

	@Test
	void missingFileAndMissingNameLeaveBufferIntact() {
		TextAreaState state = new TextAreaState(".");
		VimQueryEditor editor = new VimQueryEditor(state);
		assertThat(editor.currentFile()).isNull();
		command(editor, "w");
		assertThat(editor.statusText()).isEqualTo("No current file");
		command(editor, "saveas");
		assertThat(editor.statusText()).isEqualTo("File name required");
		command(editor, "e " + tempDir.resolve("missing.jq"));
		assertThat(editor.statusText()).startsWith("File error:");
		assertThat(state.text()).isEqualTo(".");
	}

	@Test
	void hidesOnlyTheFileTerminatingNewline() {
		TextAreaState oneLine = new TextAreaState("first\n");
		new VimQueryEditor(oneLine);
		assertThat(oneLine.text()).isEqualTo("first");
		assertThat(oneLine.lineCount()).isEqualTo(1);

		TextAreaState blankLastLine = new TextAreaState("first\n\n");
		new VimQueryEditor(blankLastLine);
		assertThat(blankLastLine.text()).isEqualTo("first\n");
		assertThat(blankLastLine.lineCount()).isEqualTo(2);

		TextAreaState windowsLines = new TextAreaState("first\r\nsecond\r\n");
		new VimQueryEditor(windowsLines);
		assertThat(windowsLines.text()).isEqualTo("first\nsecond");
		assertThat(windowsLines.lineCount()).isEqualTo(2);
	}

	@Test
	void savesOneTerminatingNewlineAndPreservesEmptyFileCases() throws Exception {
		Path file = tempDir.resolve("query.jq");
		TextAreaState state = new TextAreaState("value");
		VimQueryEditor editor = new VimQueryEditor(state);
		command(editor, "saveas " + file);
		assertThat(Files.readString(file)).isEqualTo("value\n");
		command(editor, "e");
		assertThat(state.text()).isEqualTo("value");
		assertThat(state.lineCount()).isEqualTo(1);

		Path empty = tempDir.resolve("empty.jq");
		command(new VimQueryEditor(new TextAreaState("")), "saveas " + empty);
		assertThat(Files.readString(empty)).isEmpty();

		Path newlineOnly = tempDir.resolve("newline-only.jq");
		Files.writeString(newlineOnly, "\n");
		TextAreaState blank = new TextAreaState("\n");
		VimQueryEditor blankEditor = new VimQueryEditor(blank, newlineOnly);
		assertThat(blank.text()).isEmpty();
		assertThat(blank.lineCount()).isEqualTo(1);
		command(blankEditor, "w");
		assertThat(Files.readString(newlineOnly)).isEqualTo("\n");
	}

	@Test
	void enterAtEndCreatesAVisibleBlankLine() throws Exception {
		Path file = tempDir.resolve("query.jq");
		TextAreaState state = new TextAreaState("value\n");
		VimQueryEditor editor = new VimQueryEditor(state);
		key(editor, 'A');
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		escape(editor);
		assertThat(state.text()).isEqualTo("value\n");
		assertThat(state.lineCount()).isEqualTo(2);
		command(editor, "saveas " + file);
		assertThat(Files.readString(file)).isEqualTo("value\n\n");
	}

	@Test
	void normalizesWindowsLineEndingsWhenEditingAndReading() throws Exception {
		Path file = tempDir.resolve("windows.jq");
		Files.writeString(file, "one\r\ntwo\r\n");
		TextAreaState state = new TextAreaState(".");
		VimQueryEditor editor = new VimQueryEditor(state);
		command(editor, "e " + file);
		assertThat(state.text()).isEqualTo("one\ntwo");
		command(editor, "w");
		assertThat(Files.readString(file)).isEqualTo("one\ntwo\n");

		Path extra = tempDir.resolve("extra.jq");
		Files.writeString(extra, "three\r\nfour\r\n");
		text(editor, "gg");
		command(editor, "r " + extra);
		assertThat(state.text()).isEqualTo("one\nthree\nfour\ntwo");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("one\ntwo");
		command(editor, "e");
		assertThat(editor.statusText()).startsWith("Opened:");
	}

	@Test
	void incrementallySearchesWithJavaRegularExpressions() {
		TextAreaState state = new TextAreaState("foo 12\nbar 345\nbaz 67");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg/\\d+");
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.SEARCH);
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(4);
		assertThat(editor.statusText()).isEqualTo("/\\d+ [1/3]");

		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		key(editor, 'n');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isEqualTo(4);
		key(editor, 'N');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(4);
		key(editor, 'N');
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(4);
	}

	@Test
	void cancelsInvalidSearchWithoutReplacingThePreviousSearch() {
		TextAreaState state = new TextAreaState("one two one");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg/one");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(state.cursorCol()).isEqualTo(8);

		text(editor, "/[");
		assertThat(editor.statusText()).contains("Invalid regex");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.SEARCH);
		escape(editor);
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'n');
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void clearsSearchWithControlLAndNoHighlightCommands() {
		TextAreaState state = new TextAreaState("foo foo");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "/foo");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(editor.hasSearch()).isTrue();
		editor.handleKey(KeyEvent.ofChar('l', KeyModifiers.CTRL));
		assertThat(editor.hasSearch()).isFalse();

		text(editor, "/foo");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		text(editor, ":noh");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(editor.hasSearch()).isFalse();

		text(editor, "/foo");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		text(editor, ":nohlsearch");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(editor.hasSearch()).isFalse();
	}

	@Test
	void keepsPasteOnPAndSupportsZeroWidthSearches() {
		TextAreaState state = new TextAreaState("ab ab");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "ggylp");
		assertThat(state.text()).isEqualTo("aab ab");

		text(editor, "/(?=a)");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		key(editor, 'n');
		key(editor, 'N');
		assertThat(state.cursorRow()).isZero();
	}

	@Test
	void supportsRegexFlagsAndRecomputesMatchesAfterEdits() {
		TextAreaState state = new TextAreaState("Foo\nfoo");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "gg/foo");
		assertThat(editor.statusText()).isEqualTo("/foo [1/1]");
		escape(editor);

		text(editor, "/(?im)^foo$");
		assertThat(editor.visibleSearchMatches()).hasSize(2);
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		text(editor, "dw");
		assertThat(editor.visibleSearchMatches()).hasSize(1);
		assertThat(editor.statusText()).isEqualTo("/(?im)^foo$ [1/1]");
	}

	@Test
	void leavingFocusReturnsToNormalMode() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		key(editor, 'i');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.INSERT);
		editor.leaveFocus();
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
	}

	@Test
	void supportsControlUInCommandMode() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		text(editor, ":hello");
		assertThat(editor.statusText()).isEqualTo(":hello");
		assertThat(editor.commandCursor()).isEqualTo(5);

		editor.handleKey(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		assertThat(editor.statusText()).isEqualTo(":");
		assertThat(editor.commandCursor()).isZero();
		assertThat(editor.commandText()).isEmpty();

		text(editor, "foobar");
		for (int i = 0; i < 3; i++) {
			editor.handleKey(KeyEvent.ofKey(KeyCode.LEFT));
		}
		assertThat(editor.commandCursor()).isEqualTo(3);

		editor.handleKey(KeyEvent.ofChar('u', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEqualTo("bar");
		assertThat(editor.statusText()).isEqualTo(":bar");
		assertThat(editor.commandCursor()).isZero();
	}

	@Test
	void supportsControlWInCommandMode() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		text(editor, ":hello world");
		assertThat(editor.commandCursor()).isEqualTo(11);

		editor.handleKey(KeyEvent.ofChar('w', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEqualTo("hello ");
		assertThat(editor.statusText()).isEqualTo(":hello ");
		assertThat(editor.commandCursor()).isEqualTo(6);

		text(editor, "world   ");
		assertThat(editor.commandCursor()).isEqualTo(14);
		editor.handleKey(KeyEvent.ofChar('w', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEqualTo("hello ");
		assertThat(editor.commandCursor()).isEqualTo(6);

		editor.handleKey(KeyEvent.ofChar('w', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEmpty();
		assertThat(editor.commandCursor()).isZero();
	}

	@Test
	void supportsControlKInCommandMode() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		text(editor, ":hello world");
		for (int i = 0; i < 5; i++) {
			editor.handleKey(KeyEvent.ofKey(KeyCode.LEFT));
		}
		assertThat(editor.commandCursor()).isEqualTo(6);

		editor.handleKey(KeyEvent.ofChar('k', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEqualTo("hello ");
		assertThat(editor.statusText()).isEqualTo(":hello ");
		assertThat(editor.commandCursor()).isEqualTo(6);
	}

	@Test
	void supportsNavigationAndEditingInCommandMode() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		text(editor, ":foo");
		editor.handleKey(KeyEvent.ofChar('a', KeyModifiers.CTRL));
		assertThat(editor.commandCursor()).isZero();

		text(editor, "bar");
		assertThat(editor.commandText()).isEqualTo("barfoo");
		assertThat(editor.commandCursor()).isEqualTo(3);

		editor.handleKey(KeyEvent.ofKey(KeyCode.END));
		assertThat(editor.commandCursor()).isEqualTo(6);

		for (int i = 0; i < 3; i++) {
			editor.handleKey(KeyEvent.ofKey(KeyCode.LEFT));
		}
		assertThat(editor.commandCursor()).isEqualTo(3);

		editor.handleKey(KeyEvent.ofKey(KeyCode.DELETE));
		assertThat(editor.commandText()).isEqualTo("baroo");
		assertThat(editor.commandCursor()).isEqualTo(3);

		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(editor.commandText()).isEqualTo("baoo");
		assertThat(editor.commandCursor()).isEqualTo(2);

		editor.handleKey(KeyEvent.ofChar('h', KeyModifiers.CTRL));
		assertThat(editor.commandText()).isEqualTo("boo");
		assertThat(editor.commandCursor()).isEqualTo(1);
	}

	@Test
	void backspaceInCommandModeExitsWhenEmpty() {
		VimQueryEditor editor = new VimQueryEditor(new TextAreaState("."));

		key(editor, ':');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.COMMAND);

		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);

		text(editor, ":foo");
		editor.handleKey(KeyEvent.ofKey(KeyCode.HOME));
		assertThat(editor.commandCursor()).isZero();

		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.COMMAND);
		assertThat(editor.commandText()).isEqualTo("foo");
		assertThat(editor.commandCursor()).isZero();
	}

	@Test
	void supportsControlWAndControlKInSearchMode() {
		TextAreaState state = new TextAreaState("foo bar");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "/hello world");
		editor.handleKey(KeyEvent.ofChar('w', KeyModifiers.CTRL));
		assertThat(editor.statusText()).startsWith("/hello ");

		editor.handleKey(KeyEvent.ofKey(KeyCode.HOME));
		editor.handleKey(KeyEvent.ofKey(KeyCode.RIGHT));
		editor.handleKey(KeyEvent.ofChar('k', KeyModifiers.CTRL));
		assertThat(editor.statusText()).startsWith("/h");
	}

	@Test
	void supportsSingleCharacterReplace() {
		TextAreaState state = new TextAreaState("hello");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lrx");
		assertThat(state.text()).isEqualTo("hxllo");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(1);
	}

	@Test
	void supportsCountedReplace() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0ll3rx");
		assertThat(state.text()).isEqualTo("hexxx world");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(4);
	}

	@Test
	void replacesWithNewline() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0ll");
		key(editor, 'r');
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(state.text()).isEqualTo("he\nlo world");
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void replacesCountWithSingleNewline() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0ll3r");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(state.text()).isEqualTo("he\n world");
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void replaceLastCharacterWithNewline() {
		TextAreaState state = new TextAreaState("abc");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "$r");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		assertThat(state.text()).isEqualTo("ab\n");
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void countedReplaceFailsWhenExceedingLine() {
		TextAreaState state = new TextAreaState("abc");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "04rx");
		assertThat(state.text()).isEqualTo("abc");
		assertThat(state.cursorCol()).isZero();

		text(editor, "0l3rx");
		assertThat(state.text()).isEqualTo("abc");
		assertThat(state.cursorCol()).isEqualTo(1);
	}

	@Test
	void replaceOnEmptyLineDoesNothing() {
		TextAreaState state = new TextAreaState("");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "rx");
		assertThat(state.text()).isEmpty();
	}

	@Test
	void replaceCanBeCancelled() {
		TextAreaState state = new TextAreaState("hello");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lr");
		escape(editor);
		assertThat(state.text()).isEqualTo("hello");
		assertThat(state.cursorCol()).isEqualTo(1);

		key(editor, 'r');
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(state.text()).isEqualTo("hello");

		key(editor, 'r');
		editor.handleKey(KeyEvent.ofChar('c', KeyModifiers.CTRL));
		assertThat(state.text()).isEqualTo("hello");
	}

	@Test
	void replacesWithDigitsAndSymbols() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lr5");
		assertThat(state.text()).isEqualTo("h5llo world");
		assertThat(state.cursorCol()).isEqualTo(1);

		text(editor, "l2r9");
		assertThat(state.text()).isEqualTo("h599o world");
		assertThat(state.cursorCol()).isEqualTo(3);

		text(editor, "lr ");
		assertThat(state.text()).isEqualTo("h599  world");
		assertThat(state.cursorCol()).isEqualTo(4);

		text(editor, "lr");
		editor.handleKey(KeyEvent.ofKey(KeyCode.TAB));
		assertThat(state.text()).isEqualTo("h599 \tworld");
		assertThat(state.cursorCol()).isEqualTo(5);
	}

	@Test
	void replacesUnicodeGraphemes() {
		TextAreaState state = new TextAreaState("a👨‍👩‍👧‍👦b");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lrx");
		assertThat(state.text()).isEqualTo("axb");
		assertThat(state.cursorCol()).isEqualTo(1);

		text(editor, "0r");
		editor.handleKey(KeyEvent.ofChar(0x1F600));
		assertThat(state.text()).isEqualTo("😀xb");
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void undoRestoresReplacedTextInSingleStep() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0ll3rx");
		assertThat(state.text()).isEqualTo("hexxx world");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("hello world");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(2);
	}

	@Test
	void replaceDoesNotAffectRegisters() {
		TextAreaState state = new TextAreaState("abc");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0yyrx");
		assertThat(state.text()).isEqualTo("xbc");

		key(editor, 'p');
		assertThat(state.text()).isEqualTo("xbc\nabc");
	}

	@Test
	void entersAndExitsReplaceMode() {
		TextAreaState state = new TextAreaState("hello");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0l");
		key(editor, 'R');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.REPLACE);
		assertThat(editor.modeLabel()).isEqualTo("REPLACE");

		escape(editor);
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
		assertThat(editor.modeLabel()).isEqualTo("NORMAL");
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void replacesCharactersSequentiallyInReplaceMode() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lRxyz");
		escape(editor);

		assertThat(state.text()).isEqualTo("hxyzo world");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(3);
	}

	@Test
	void appendsCharactersPastEndOfLineInReplaceMode() {
		TextAreaState state = new TextAreaState("hi");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0Rabcdef");
		escape(editor);

		assertThat(state.text()).isEqualTo("abcdef");
		assertThat(state.cursorCol()).isEqualTo(5);
	}

	@Test
	void backspaceRestoresOverwrittenCharactersInReplaceMode() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lRxyz");
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		escape(editor);

		assertThat(state.text()).isEqualTo("hxllo world");
		assertThat(state.cursorCol()).isEqualTo(1);
	}

	@Test
	void backspaceDeletesAppendedCharactersInReplaceMode() {
		TextAreaState state = new TextAreaState("hi");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0Rabcdef");
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		escape(editor);

		assertThat(state.text()).isEqualTo("abcd");
		assertThat(state.cursorCol()).isEqualTo(3);
	}

	@Test
	void backspacePastOriginalCursorMovesLeftInReplaceMode() {
		TextAreaState state = new TextAreaState("hello");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0llR");
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(state.cursorCol()).isEqualTo(1);
		assertThat(state.text()).isEqualTo("hello");
	}

	@Test
	void replaceModeSplitsLineOnEnter() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0llRxy");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		text(editor, "z");
		escape(editor);

		assertThat(state.text()).isEqualTo("hexy\nz world");
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();
	}

	@Test
	void backspaceReversesEnterNewlineInReplaceMode() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0llRxy");
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		escape(editor);

		assertThat(state.text()).isEqualTo("hexyo world");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(3);
	}

	@Test
	void replacesExtendedGraphemesInReplaceMode() {
		TextAreaState state = new TextAreaState("a👨‍👩‍👧‍👦b");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lRX");
		assertThat(state.text()).isEqualTo("aXb");
		assertThat(state.cursorCol()).isEqualTo(2);

		editor.handleKey(KeyEvent.ofKey(KeyCode.BACKSPACE));
		assertThat(state.text()).isEqualTo("a👨‍👩‍👧‍👦b");
		assertThat(state.cursorCol()).isEqualTo(1);
	}

	@Test
	void undoRestoresEntireReplaceSession() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lRxyz");
		escape(editor);
		assertThat(state.text()).isEqualTo("hxyzo world");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("hello world");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(1);
	}

	@Test
	void entersAndExitsCharacterwiseVisualMode() {
		TextAreaState state = new TextAreaState("hello world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0w");
		key(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL);
		assertThat(editor.modeLabel()).isEqualTo("VISUAL");
		assertThat(editor.isVisualMode()).isTrue();
		assertThat(editor.statusText()).isEqualTo("1 character");

		text(editor, "ll");
		assertThat(editor.statusText()).isEqualTo("3 characters");

		key(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
		assertThat(editor.isVisualMode()).isFalse();

		key(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL);
		escape(editor);
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
	}

	@Test
	void characterwiseVisualDeletesAndYanks() {
		TextAreaState state = new TextAreaState("hello beautiful world");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0wve");
		key(editor, 'y');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
		assertThat(state.text()).isEqualTo("hello beautiful world");

		text(editor, "0wved");
		assertThat(state.text()).isEqualTo("hello  world");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("hello beautiful world");

		text(editor, "$p");
		assertThat(state.text()).contains("beautiful");
	}

	@Test
	void characterwiseVisualChangesAndUndoes() {
		TextAreaState state = new TextAreaState("foo.bar");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0ve");
		key(editor, 'c');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.INSERT);
		text(editor, "baz");
		escape(editor);

		assertThat(state.text()).isEqualTo("baz.bar");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("foo.bar");
	}

	@Test
	void characterwiseVisualToggleEndpointsWithO() {
		TextAreaState state = new TextAreaState("abcdef");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0lvll");
		assertThat(state.cursorCol()).isEqualTo(3);
		key(editor, 'o');
		assertThat(state.cursorCol()).isEqualTo(1);
		key(editor, 'h');
		assertThat(state.cursorCol()).isZero();
		key(editor, 'o');
		assertThat(state.cursorCol()).isEqualTo(3);
		key(editor, 'd');
		assertThat(state.text()).isEqualTo("ef");
	}

	@Test
	void characterwiseVisualReplaceAndCaseConversion() {
		TextAreaState state = new TextAreaState("hello WORLD");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0vl");
		key(editor, '~');
		assertThat(state.text()).isEqualTo("HEllo WORLD");

		text(editor, "0vll");
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("hello WORLD");

		text(editor, "0vll");
		key(editor, 'U');
		assertThat(state.text()).isEqualTo("HELlo WORLD");

		text(editor, "0vl");
		text(editor, "rx");
		assertThat(state.text()).isEqualTo("xxLlo WORLD");
	}

	@Test
	void characterwiseVisualPastesOverSelection() {
		TextAreaState state = new TextAreaState("one two three");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0vey"); // yank "one"
		text(editor, "wvep"); // select "two" and paste "one" over it
		assertThat(state.text()).isEqualTo("one one three");

		text(editor, "wvep"); // paste again (register now has "two"!)
		assertThat(state.text()).isEqualTo("one one two");
	}

	@Test
	void entersAndExitsLinewiseVisualMode() {
		TextAreaState state = new TextAreaState("line 1\nline 2\nline 3");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		key(editor, 'V');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_LINE);
		assertThat(editor.modeLabel()).isEqualTo("VISUAL LINE");
		assertThat(editor.statusText()).isEqualTo("1 line");

		key(editor, 'j');
		assertThat(editor.statusText()).isEqualTo("2 lines");

		key(editor, 'V');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);

		shiftKey(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_LINE);
		escape(editor);
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
	}

	@Test
	void linewiseVisualDeletesAndYanks() {
		TextAreaState state = new TextAreaState("a\nb\nc\nd");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "jVj");
		key(editor, 'y');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
		assertThat(state.text()).isEqualTo("a\nb\nc\nd");

		text(editor, "Vjd");
		assertThat(state.text()).isEqualTo("a\nd");

		key(editor, 'P');
		assertThat(state.text()).isEqualTo("a\nb\nc\nd");

		key(editor, 'u');
		key(editor, 'u');
		assertThat(state.text()).isEqualTo("a\nb\nc\nd");
	}

	@Test
	void linewiseVisualIndentsAndOutdents() {
		TextAreaState state = new TextAreaState("foo\nbar\nbaz");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "Vj>");
		assertThat(state.text()).isEqualTo("  foo\n  bar\nbaz");

		text(editor, "Vj<");
		assertThat(state.text()).isEqualTo("foo\nbar\nbaz");
	}

	@Test
	void linewiseVisualJoinsLines() {
		TextAreaState state = new TextAreaState("foo\n  bar\n  baz");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "VjJ");
		assertThat(state.text()).isEqualTo("foo bar\n  baz");
	}

	@Test
	void linewiseVisualChanges() {
		TextAreaState state = new TextAreaState("first\nsecond\nthird");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "jVc");
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.INSERT);
		text(editor, "replacement");
		escape(editor);
		assertThat(state.text()).isEqualTo("first\nreplacement\nthird");
	}

	@Test
	void entersAndExitsBlockwiseVisualMode() {
		TextAreaState state = new TextAreaState("1234\n5678\nabcd");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		ctrlKey(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_BLOCK);
		assertThat(editor.modeLabel()).isEqualTo("VISUAL BLOCK");
		assertThat(editor.statusText()).isEqualTo("1x1");

		text(editor, "2jl");
		assertThat(editor.statusText()).isEqualTo("3x2");

		ctrlKey(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);

		ctrlKey(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_BLOCK);
		escape(editor);
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
	}

	@Test
	void blockwiseVisualDeletesAndYanks() {
		TextAreaState state = new TextAreaState("abcdef\nabcdef\nabcdef");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0l");
		ctrlKey(editor, 'v');
		text(editor, "j2l");
		key(editor, 'd');
		assertThat(state.text()).isEqualTo("aef\naef\nabcdef");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("abcdef\nabcdef\nabcdef");
	}

	@Test
	void blockwiseVisualPastesBlockOnSuccessiveLines() {
		TextAreaState state = new TextAreaState("xx\nyy\nzz");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0");
		ctrlKey(editor, 'v');
		text(editor, "j");
		key(editor, 'y'); // yank "x\ny" block

		text(editor, "$p"); // paste block at end of lines
		assertThat(state.text()).isEqualTo("xxx\nyyy\nzz");
	}

	@Test
	void blockwiseVisualInsertReplicatesAcrossRows() {
		TextAreaState state = new TextAreaState("apple\nbanana\ncherry");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0");
		ctrlKey(editor, 'v');
		text(editor, "2j");
		key(editor, 'I');
		text(editor, "# ");
		escape(editor);

		assertThat(state.text()).isEqualTo("# apple\n# banana\n# cherry");

		key(editor, 'u');
		assertThat(state.text()).isEqualTo("apple\nbanana\ncherry");
	}

	@Test
	void blockwiseVisualAppendReplicatesAcrossRows() {
		TextAreaState state = new TextAreaState("one\ntwo\nsix");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0");
		ctrlKey(editor, 'v');
		text(editor, "2j2l");
		key(editor, 'A');
		text(editor, ";");
		escape(editor);

		assertThat(state.text()).isEqualTo("one;\ntwo;\nsix;");
	}

	@Test
	void blockwiseVisualChangeReplicatesAcrossRows() {
		TextAreaState state = new TextAreaState("var1 = 1\nvar2 = 2\nvar3 = 3");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0");
		ctrlKey(editor, 'v');
		text(editor, "2j3l");
		key(editor, 'c');
		text(editor, "val");
		escape(editor);

		assertThat(state.text()).isEqualTo("val = 1\nval = 2\nval = 3");
	}

	@Test
	void blockwiseVisualToggleEndpointsWithOAndCapitalO() {
		TextAreaState state = new TextAreaState("12345\n67890");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0");
		ctrlKey(editor, 'v');
		text(editor, "j2l");
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isEqualTo(2);

		key(editor, 'O'); // horizontal opposite on same line
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();

		key(editor, 'o'); // diagonal opposite
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(2);
	}

	@Test
	void switchesBetweenVisualModesDirectly() {
		TextAreaState state = new TextAreaState("hello\nworld");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		key(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL);

		key(editor, 'V');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_LINE);

		ctrlKey(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL_BLOCK);

		key(editor, 'v');
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.VISUAL);

		escape(editor);
		assertThat(editor.mode()).isEqualTo(VimQueryEditor.Mode.NORMAL);
	}

	@Test
	void supportsTextObjectsInVisualMode() {
		TextAreaState state = new TextAreaState("foo(\"hello world\")");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0fhl");
		text(editor, "vi\"");
		key(editor, 'd');
		assertThat(state.text()).isEqualTo("foo(\"\")");

		key(editor, 'u');
		text(editor, "0fhl");
		text(editor, "va\"");
		key(editor, 'd');
		assertThat(state.text()).isEqualTo("foo()");
	}

	@Test
	void supportsWordTextObjectsInOperatorPendingMode() {
		TextAreaState state = new TextAreaState("foo bar baz");
		VimQueryEditor editor = new VimQueryEditor(state);

		text(editor, "0wdiw");
		assertThat(state.text()).isEqualTo("foo  baz");

		key(editor, 'u');
		text(editor, "0wdaw");
		assertThat(state.text()).isEqualTo("foo baz");
	}

	@Test
	void computesVisualRangeOnRowCorrectly() {
		TextAreaState state = new TextAreaState("abcd\nefgh\nijkl");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");

		text(editor, "0l");
		key(editor, 'v');
		text(editor, "j");
		VimQueryEditor.VisualRange r0 = editor.visualRangeOnRow(0);
		assertThat(r0).isNotNull();
		assertThat(Objects.requireNonNull(r0).startCol()).isEqualTo(1);
		assertThat(r0.endCol()).isEqualTo(4);
		assertThat(r0.includesNewline()).isTrue();

		VimQueryEditor.VisualRange r1 = editor.visualRangeOnRow(1);
		assertThat(r1).isNotNull();
		assertThat(Objects.requireNonNull(r1).startCol()).isZero();
		assertThat(r1.endCol()).isEqualTo(2);

		assertThat(editor.visualRangeOnRow(2)).isNull();

		key(editor, 'V');
		VimQueryEditor.VisualRange lineRange = editor.visualRangeOnRow(0);
		assertThat(lineRange).isNotNull();
		assertThat(Objects.requireNonNull(lineRange).startCol()).isZero();
		assertThat(lineRange.endCol()).isEqualTo(4);
		assertThat(lineRange.includesNewline()).isTrue();

		ctrlKey(editor, 'v');
		VimQueryEditor.VisualRange blockRange = editor.visualRangeOnRow(0);
		assertThat(blockRange).isNotNull();
		assertThat(Objects.requireNonNull(blockRange).startCol()).isEqualTo(1);
		assertThat(blockRange.endCol()).isEqualTo(2);
		assertThat(blockRange.includesNewline()).isFalse();
	}

	@Test
	void preservesPreferredColumnOnVerticalMotionAcrossEmptyAndShorterLines() {
		TextAreaState state = new TextAreaState("0123456789\n\n0123456789");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		text(editor, "8l");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'j');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();

		key(editor, 'j');
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'k');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isZero();

		key(editor, 'k');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(8);

		TextAreaState state2 = new TextAreaState("0123456789\n01234\n0123456789");
		VimQueryEditor editor2 = new VimQueryEditor(state2);
		text(editor2, "gg0");
		text(editor2, "8l");

		key(editor2, 'j');
		assertThat(state2.cursorRow()).isEqualTo(1);
		assertThat(state2.cursorCol()).isEqualTo(4);

		key(editor2, 'j');
		assertThat(state2.cursorRow()).isEqualTo(2);
		assertThat(state2.cursorCol()).isEqualTo(8);
	}

	@Test
	void horizontalMotionUpdatesPreferredColumn() {
		TextAreaState state = new TextAreaState("0123456789\n\n0123456789");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		text(editor, "8l");
		text(editor, "jj");
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'h');
		assertThat(state.cursorCol()).isEqualTo(7);

		text(editor, "kk");
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(7);
	}

	@Test
	void dollarMotionTracksEndOfLinesOfVaryingLengths() {
		TextAreaState state = new TextAreaState("12345\n1234567890\n12");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");

		key(editor, '$');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(4);

		key(editor, 'j');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isEqualTo(9);

		key(editor, 'j');
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(1);

		key(editor, 'k');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isEqualTo(9);

		key(editor, 'k');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(4);

		key(editor, 'h');
		assertThat(state.cursorCol()).isEqualTo(3);

		key(editor, 'j');
		assertThat(state.cursorRow()).isEqualTo(1);
		assertThat(state.cursorCol()).isEqualTo(3);
	}

	@Test
	void preservesPreferredColumnInCharacterwiseVisualMode() {
		TextAreaState state = new TextAreaState("0123456789\n\n0123456789");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		text(editor, "8l");
		key(editor, 'v');
		text(editor, "jj");

		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);

		VimQueryEditor.VisualRange r0 = editor.visualRangeOnRow(0);
		assertThat(r0).isNotNull();
		assertThat(Objects.requireNonNull(r0).startCol()).isEqualTo(8);
		assertThat(r0.endCol()).isEqualTo(10);
		assertThat(r0.includesNewline()).isTrue();

		VimQueryEditor.VisualRange r1 = editor.visualRangeOnRow(1);
		assertThat(r1).isNotNull();
		assertThat(Objects.requireNonNull(r1).startCol()).isZero();
		assertThat(r1.endCol()).isZero();
		assertThat(r1.includesNewline()).isTrue();

		VimQueryEditor.VisualRange r2 = editor.visualRangeOnRow(2);
		assertThat(r2).isNotNull();
		assertThat(Objects.requireNonNull(r2).startCol()).isZero();
		assertThat(r2.endCol()).isEqualTo(9);
		assertThat(r2.includesNewline()).isFalse();

		key(editor, 'y');
		text(editor, "Go");
		escape(editor);
		key(editor, 'p');
		assertThat(state.text()).isEqualTo("0123456789\n\n0123456789\n89\n\n012345678");
	}

	@Test
	void preservesPreferredColumnInVisualBlockMode() {
		TextAreaState state = new TextAreaState("0123456789\n\n0123456789");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		text(editor, "8l");
		ctrlKey(editor, 'v');
		text(editor, "jj");

		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);

		VimQueryEditor.VisualRange r0 = editor.visualRangeOnRow(0);
		assertThat(r0).isNotNull();
		assertThat(Objects.requireNonNull(r0).startCol()).isEqualTo(8);
		assertThat(r0.endCol()).isEqualTo(9);
		assertThat(r0.includesNewline()).isFalse();

		VimQueryEditor.VisualRange r1 = editor.visualRangeOnRow(1);
		assertThat(r1).isNotNull();
		assertThat(Objects.requireNonNull(r1).startCol()).isZero();
		assertThat(r1.endCol()).isZero();
		assertThat(r1.includesNewline()).isFalse();

		VimQueryEditor.VisualRange r2 = editor.visualRangeOnRow(2);
		assertThat(r2).isNotNull();
		assertThat(Objects.requireNonNull(r2).startCol()).isEqualTo(8);
		assertThat(r2.endCol()).isEqualTo(9);
		assertThat(r2.includesNewline()).isFalse();

		key(editor, 'y');
		text(editor, "Go");
		escape(editor);
		key(editor, 'p');
		assertThat(state.text()).isEqualTo("0123456789\n\n0123456789\n8\n\n8");
	}

	@Test
	void visualBlockModeTracksEndOfLinesWithDollar() {
		TextAreaState state = new TextAreaState("12345\n1234567890\n12");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		ctrlKey(editor, 'v');
		key(editor, '$');
		text(editor, "2j");

		VimQueryEditor.VisualRange r0 = editor.visualRangeOnRow(0);
		assertThat(r0).isNotNull();
		assertThat(Objects.requireNonNull(r0).startCol()).isZero();
		assertThat(r0.endCol()).isEqualTo(5);

		VimQueryEditor.VisualRange r1 = editor.visualRangeOnRow(1);
		assertThat(r1).isNotNull();
		assertThat(Objects.requireNonNull(r1).startCol()).isZero();
		assertThat(r1.endCol()).isEqualTo(10);

		VimQueryEditor.VisualRange r2 = editor.visualRangeOnRow(2);
		assertThat(r2).isNotNull();
		assertThat(Objects.requireNonNull(r2).startCol()).isZero();
		assertThat(r2.endCol()).isEqualTo(2);

		key(editor, 'y');
		text(editor, "Go");
		escape(editor);
		key(editor, 'p');
		assertThat(state.text()).isEqualTo("12345\n1234567890\n12\n12345\n1234567890\n12");
	}

	@Test
	void undoRestoresPreferredColumn() {
		TextAreaState state = new TextAreaState("0123456789\n\n0123456789");
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg0");
		text(editor, "8l");

		text(editor, "rx");
		text(editor, "jj");
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);

		key(editor, 'u');
		assertThat(state.cursorRow()).isZero();
		assertThat(state.cursorCol()).isEqualTo(8);

		text(editor, "jj");
		assertThat(state.cursorRow()).isEqualTo(2);
		assertThat(state.cursorCol()).isEqualTo(8);
	}

	private static void text(VimQueryEditor editor, String text) {
		for (int i = 0; i < text.length(); i++) {
			key(editor, text.charAt(i));
		}
	}

	private static void command(VimQueryEditor editor, String command) {
		key(editor, ':');
		text(editor, command);
		editor.handleKey(KeyEvent.ofKey(KeyCode.ENTER));
	}

	private static void assertTextObject(String input, String command, String expected) {
		TextAreaState state = new TextAreaState(input);
		VimQueryEditor editor = new VimQueryEditor(state);
		text(editor, "gg");
		int cursor = input.indexOf('x');
		for (int i = 0; i < cursor; i++) {
			key(editor, 'l');
		}
		text(editor, command);
		assertThat(state.text()).isEqualTo(expected);
	}

	private static void key(VimQueryEditor editor, char key) {
		editor.handleKey(KeyEvent.ofChar(key));
	}

	private static void ctrlKey(VimQueryEditor editor, char key) {
		editor.handleKey(KeyEvent.ofChar(key, KeyModifiers.CTRL));
	}

	private static void shiftKey(VimQueryEditor editor, char key) {
		editor.handleKey(KeyEvent.ofChar(key, KeyModifiers.SHIFT));
	}

	private static void escape(VimQueryEditor editor) {
		editor.handleKey(KeyEvent.ofKey(KeyCode.ESCAPE));
	}
}
