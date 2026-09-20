package net.thisptr.jackson.jq.v2.cli;

import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTextHighlighterTest {

	@Test
	void emptyLine() {
		Line line = JsonTextHighlighter.highlightLine("", "", false);
		assertThat(line.spans()).isEmpty();
	}

	@Test
	void specialLines() {
		Line noInput = JsonTextHighlighter.highlightLine("(no input)", "", false);
		assertThat(noInput.spans()).hasSize(1);
		assertThat(noInput.spans().get(0).content()).isEqualTo("(no input)");
		assertThat(noInput.spans().get(0).style()).isEqualTo(Style.EMPTY.dim());

		Line error = JsonTextHighlighter.highlightLine("(error parsing input: bad json)", "", false);
		assertThat(error.spans()).hasSize(1);
		assertThat(error.spans().get(0).content()).isEqualTo("(error parsing input: bad json)");
		assertThat(error.spans().get(0).style()).isEqualTo(Style.EMPTY.red());
	}

	@Test
	void keyAndStringValue() {
		Line line = JsonTextHighlighter.highlightLine("  \"title\": \"Hello World\",", "", false);
		List<Span> spans = line.spans();
		assertThat(spans).hasSize(6);

		assertThat(spans.get(0).content()).isEqualTo("  ");
		assertThat(spans.get(0).style()).isEqualTo(Style.EMPTY);

		assertThat(spans.get(1).content()).isEqualTo("\"title\"");
		assertThat(spans.get(1).style()).isEqualTo(Style.EMPTY.bold().cyan());

		assertThat(spans.get(2).content()).isEqualTo(":");
		assertThat(spans.get(2).style()).isEqualTo(Style.EMPTY.white());

		assertThat(spans.get(3).content()).isEqualTo(" ");
		assertThat(spans.get(3).style()).isEqualTo(Style.EMPTY);

		assertThat(spans.get(4).content()).isEqualTo("\"Hello World\"");
		assertThat(spans.get(4).style()).isEqualTo(Style.EMPTY.green());

		assertThat(spans.get(5).content()).isEqualTo(",");
		assertThat(spans.get(5).style()).isEqualTo(Style.EMPTY.white());
	}

	@Test
	void numbersBooleansAndNull() {
		Line line = JsonTextHighlighter.highlightLine("[-12, 3.14, 1e-4, true, false, null]", "", false);
		List<Span> spans = line.spans();

		// [
		assertThat(spans.get(0).content()).isEqualTo("[");
		assertThat(spans.get(0).style()).isEqualTo(Style.EMPTY.white());

		// -12
		assertThat(spans.get(1).content()).isEqualTo("-12");
		assertThat(spans.get(1).style()).isEqualTo(Style.EMPTY.yellow());

		// 3.14
		assertThat(spans.get(4).content()).isEqualTo("3.14");
		assertThat(spans.get(4).style()).isEqualTo(Style.EMPTY.yellow());

		// 1e-4
		assertThat(spans.get(7).content()).isEqualTo("1e-4");
		assertThat(spans.get(7).style()).isEqualTo(Style.EMPTY.yellow());

		// true
		assertThat(spans.get(10).content()).isEqualTo("true");
		assertThat(spans.get(10).style()).isEqualTo(Style.EMPTY.magenta());

		// false
		assertThat(spans.get(13).content()).isEqualTo("false");
		assertThat(spans.get(13).style()).isEqualTo(Style.EMPTY.magenta());

		// null
		assertThat(spans.get(16).content()).isEqualTo("null");
		assertThat(spans.get(16).style()).isEqualTo(Style.EMPTY.dim());

		// ]
		assertThat(spans.get(17).content()).isEqualTo("]");
		assertThat(spans.get(17).style()).isEqualTo(Style.EMPTY.white());
	}

	@Test
	void searchMatchInsideString() {
		Line line = JsonTextHighlighter.highlightLine("  \"msg\": \"hello world\",", "world", false);
		List<Span> spans = line.spans();

		// Find the spans around "hello world"
		// Token was "\"hello world\"" -> should be sliced into "\"hello ", "world", "\""
		Span matchSpan = spans.stream().filter(s -> s.content().equals("world")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchSpan.style()).isEqualTo(Style.EMPTY.bold().bg(Color.YELLOW).fg(Color.BLACK));

		Span prefixSpan = spans.stream().filter(s -> s.content().equals("\"hello ")).findFirst().orElseThrow(AssertionError::new);
		assertThat(prefixSpan.style()).isEqualTo(Style.EMPTY.green());

		Span suffixSpan = spans.stream().filter(s -> s.content().equals("\"")).findFirst().orElseThrow(AssertionError::new);
		assertThat(suffixSpan.style()).isEqualTo(Style.EMPTY.green());
	}

	@Test
	void searchMatchAcrossTokens() {
		Line line = JsonTextHighlighter.highlightLine("  \"count\": 42", "t\": 4", false);
		List<Span> spans = line.spans();

		// "count" was [2, 9). Match is [7, 12).
		// Prefix of "count" before match is "\"coun" with bold cyan
		Span prefixKey = spans.stream().filter(s -> s.content().equals("\"coun")).findFirst().orElseThrow(AssertionError::new);
		assertThat(prefixKey.style()).isEqualTo(Style.EMPTY.bold().cyan());

		// Suffix of 42 after match is "2" with yellow
		Span suffixNum = spans.stream().filter(s -> s.content().equals("2")).findFirst().orElseThrow(AssertionError::new);
		assertThat(suffixNum.style()).isEqualTo(Style.EMPTY.yellow());

		// The matched parts should have matchStyle
		Style matchStyle = Style.EMPTY.bold().bg(Color.YELLOW).fg(Color.BLACK);
		Span matchedKeyPart = spans.stream().filter(s -> s.content().equals("t\"")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchedKeyPart.style()).isEqualTo(matchStyle);

		Span matchedColon = spans.stream().filter(s -> s.content().equals(":")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchedColon.style()).isEqualTo(matchStyle);

		Span matchedSpace = spans.stream().filter(s -> s.content().equals(" ")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchedSpace.style()).isEqualTo(matchStyle);

		Span matchedNumPart = spans.stream().filter(s -> s.content().equals("4")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchedNumPart.style()).isEqualTo(matchStyle);
	}

	@Test
	void activeSearchMatchUsesMagenta() {
		Line line = JsonTextHighlighter.highlightLine("  \"item\": true", "true", true);
		Span matchSpan = line.spans().stream().filter(s -> s.content().equals("true")).findFirst().orElseThrow(AssertionError::new);
		assertThat(matchSpan.style()).isEqualTo(Style.EMPTY.bold().bg(Color.MAGENTA).fg(Color.WHITE));
	}

	@Test
	void noMatchRetainsOriginalHighlighting() {
		Line line = JsonTextHighlighter.highlightLine("  \"active\": true", "notfound", false);
		Span boolSpan = line.spans().stream().filter(s -> s.content().equals("true")).findFirst().orElseThrow(AssertionError::new);
		assertThat(boolSpan.style()).isEqualTo(Style.EMPTY.magenta());
	}
}
