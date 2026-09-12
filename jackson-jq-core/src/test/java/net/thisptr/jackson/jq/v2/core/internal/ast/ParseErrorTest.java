package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParseErrorTest {
	// Shaped after jq's own syntax errors, minus the source-name field jq prints (`<top-level>` or
	// the module path), which this parser does not track.
	@Test
	void aSyntaxErrorNamesTheTokenAndPointsAtIt() {
		assertThatThrownBy(() -> parse("[1] | .[1:2:3]"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageStartingWith("syntax error, unexpected ':', expecting ")
				.hasMessageContaining(" at line 1, column 12:\n")
				.hasMessageEndingWith("    [1] | .[1:2:3]\n               ^");
	}

	@Test
	void runningOutOfInputIsSaidInWords() {
		assertThatThrownBy(() -> parse(".foo |"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageStartingWith("syntax error, unexpected end of input, expecting ")
				.hasMessageEndingWith("    .foo |\n         ^");
	}

	@Test
	void theCaretFindsTheRightLineOfAMultiLineQuery() {
		assertThatThrownBy(() -> parse(".foo\n| .bar\n| ["))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining(" at line 3, column 3:\n")
				.hasMessageEndingWith("    | [\n      ^");
	}

	// The grammar's own semantic checks explain themselves; their message is what the caller sees,
	// rather than being flattened into a generic "cannot parse" line.
	@Test
	void aSemanticErrorKeepsItsOwnMessage() {
		assertThatThrownBy(() -> parse("1 as $x"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessage("Assignment or label must be followed by pipes: 1 as $x");
	}

	@Test
	void aLexicalErrorKeepsItsPosition() {
		assertThatThrownBy(() -> parse("@"))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("line 1, column 1");
	}

	private static AstNode parse(String query) throws JsonQueryException {
		return AstParser.parse(query, Versions.JQ_1_7);
	}
}
