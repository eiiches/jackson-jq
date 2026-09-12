package net.thisptr.jackson.jq.v2.core.internal.ast;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SourceLocationTest {
	@Test
	void everyNodeCoversExactlyTheTextItWasParsedFrom() throws JsonQueryException {
		PipeAstNode pipe = assertInstanceOf(PipeAstNode.class, parse(".foo | .bar"));
		assertThat(pipe.location()).isEqualTo(SourceLocation.of(1, 1, 1, 11));
		assertThat(pipe.left().location()).isEqualTo(SourceLocation.of(1, 1, 1, 4));
		assertThat(pipe.right().location()).isEqualTo(SourceLocation.of(1, 8, 1, 11));
	}

	// A pipe head covers only the text of the head itself; the body it scopes belongs to the pipe.
	@Test
	void aPipeHeadCoversOnlyItself() throws JsonQueryException {
		PipeAstNode binding = assertInstanceOf(PipeAstNode.class, parse(". as $x | $x"));
		assertThat(binding.location()).isEqualTo(SourceLocation.of(1, 1, 1, 12));
		assertThat(binding.left().location()).isEqualTo(SourceLocation.of(1, 1, 1, 7));
		assertThat(binding.right().location()).isEqualTo(SourceLocation.of(1, 11, 1, 12));

		PipeAstNode label = assertInstanceOf(PipeAstNode.class, parse("label $out | ."));
		assertThat(label.location()).isEqualTo(SourceLocation.of(1, 1, 1, 14));
		assertThat(label.left().location()).isEqualTo(SourceLocation.of(1, 1, 1, 10));
		assertThat(label.right().location()).isEqualTo(SourceLocation.of(1, 14, 1, 14));
	}

	// A field access covers the target it applies to, not just its own name: `.foo` starts at the
	// dot, and `.a.b` starts where `.a` does.
	@Test
	void fieldAccessCoversItsTarget() throws JsonQueryException {
		assertThat(parse(".foo").location()).isEqualTo(SourceLocation.of(1, 1, 1, 4));
		assertThat(parse(".a.b").location()).isEqualTo(SourceLocation.of(1, 1, 1, 4));
		assertThat(parse(".[]").location()).isEqualTo(SourceLocation.of(1, 1, 1, 3));
	}

	@Test
	void constructionsCoverTheirBrackets() throws JsonQueryException {
		assertThat(parse("[1, 2]").location()).isEqualTo(SourceLocation.of(1, 1, 1, 6));
		assertThat(parse("{a: 1}").location()).isEqualTo(SourceLocation.of(1, 1, 1, 6));
	}

	@Test
	void binaryOperatorCoversBothOperands() throws JsonQueryException {
		assertThat(parse("1 + 2 * 3").location()).isEqualTo(SourceLocation.of(1, 1, 1, 9));
	}

	@Test
	void aCommaBindsTighterThanAPipe() throws JsonQueryException {
		PipeAstNode pipe = assertInstanceOf(PipeAstNode.class, parse("a, b | ."));
		TupleAstNode tuple = assertInstanceOf(TupleAstNode.class, pipe.left());
		assertThat(tuple.location()).isEqualTo(SourceLocation.of(1, 1, 1, 4));
		assertThat(pipe.right().location()).isEqualTo(SourceLocation.of(1, 8, 1, 8));
	}

	@Test
	void locationsTrackLineNumbers() throws JsonQueryException {
		PipeAstNode pipe = assertInstanceOf(PipeAstNode.class, parse(".foo\n| .bar"));
		assertThat(pipe.right().location()).isEqualTo(SourceLocation.of(2, 3, 2, 6));
	}

	@Test
	void excerptPointsAtTheOffendingColumn() {
		assertThat(SourceLocation.of(1, 6).excerpt("[1] | .[1:2]"))
				.isEqualTo("    [1] | .[1:2]\n         ^");
	}

	@Test
	void excerptPicksTheLineTheLocationBeginsOn() {
		assertThat(SourceLocation.of(2, 3).excerpt(".foo\n| .bar"))
				.isEqualTo("    | .bar\n      ^");
		assertThat(SourceLocation.of(3, 1).excerpt(".foo")).isNull();
	}

	private static AstNode parse(String query) throws JsonQueryException {
		return AstParser.parse(query, Versions.JQ_1_7);
	}
}
