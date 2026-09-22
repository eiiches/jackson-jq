package net.thisptr.jackson.jq.v2.cli;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.widgets.input.TextAreaState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VimQueryEditorTest {
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

	private static void text(VimQueryEditor editor, String text) {
		for (int i = 0; i < text.length(); i++) {
			key(editor, text.charAt(i));
		}
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

	private static void escape(VimQueryEditor editor) {
		editor.handleKey(KeyEvent.ofKey(KeyCode.ESCAPE));
	}
}
