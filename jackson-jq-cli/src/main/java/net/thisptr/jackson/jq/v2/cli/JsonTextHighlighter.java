package net.thisptr.jackson.jq.v2.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

/**
 * Provides syntax highlighting for JSON text lines in the TUI playground's text view mode,
 * with search match overlay support.
 */
final class JsonTextHighlighter {
	private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

	private JsonTextHighlighter() {
	}

	static final class Token {
		final int start;
		final int end;
		final Style style;

		Token(int start, int end, Style style) {
			this.start = start;
			this.end = end;
			this.style = style;
		}

		int start() {
			return start;
		}

		int end() {
			return end;
		}

		Style style() {
			return style;
		}
	}

	static List<Token> tokenize(String line) {
		List<Token> tokens = new ArrayList<>();
		int n = line.length();
		@Var int i = 0;

		String trimmed = line.trim();
		if (trimmed.startsWith("(error")) {
			tokens.add(new Token(0, n, Style.EMPTY.red()));
			return tokens;
		} else if (trimmed.startsWith("(no input)")) {
			tokens.add(new Token(0, n, Style.EMPTY.dim()));
			return tokens;
		}

		while (i < n) {
			char c = line.charAt(i);

			if (Character.isWhitespace(c)) {
				int start = i;
				while (i < n && Character.isWhitespace(line.charAt(i))) {
					i++;
				}
				tokens.add(new Token(start, i, Style.EMPTY));
			} else if (c == '"') {
				int start = i;
				i++;
				while (i < n) {
					char ch = line.charAt(i);
					if (ch == '\\') {
						i += 2;
					} else if (ch == '"') {
						i++;
						break;
					} else {
						i++;
					}
				}
				if (i > n) {
					i = n;
				}
				@Var int lookahead = i;
				while (lookahead < n && Character.isWhitespace(line.charAt(lookahead))) {
					lookahead++;
				}
				boolean isKey = lookahead < n && line.charAt(lookahead) == ':';
				Style style = isKey ? Style.EMPTY.bold().cyan() : Style.EMPTY.green();
				tokens.add(new Token(start, i, style));
			} else if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
				tokens.add(new Token(i, i + 1, Style.EMPTY.white()));
				i++;
			} else if (line.startsWith("true", i) && isWordBoundary(line, i + 4)) {
				tokens.add(new Token(i, i + 4, Style.EMPTY.magenta()));
				i += 4;
			} else if (line.startsWith("false", i) && isWordBoundary(line, i + 5)) {
				tokens.add(new Token(i, i + 5, Style.EMPTY.magenta()));
				i += 5;
			} else if (line.startsWith("null", i) && isWordBoundary(line, i + 4)) {
				tokens.add(new Token(i, i + 4, Style.EMPTY.dim()));
				i += 4;
			} else if (c == '-' || (c >= '0' && c <= '9')) {
				Matcher m = NUMBER_PATTERN.matcher(line.substring(i));
				if (m.find() && m.start() == 0 && isWordBoundary(line, i + m.end())) {
					int len = m.end();
					tokens.add(new Token(i, i + len, Style.EMPTY.yellow()));
					i += len;
				} else {
					tokens.add(new Token(i, i + 1, Style.EMPTY));
					i++;
				}
			} else {
				int start = i;
				while (i < n) {
					char ch = line.charAt(i);
					if (Character.isWhitespace(ch) || ch == '"' || ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == ':' || ch == ',') {
						break;
					}
					i++;
				}
				tokens.add(new Token(start, i, Style.EMPTY));
			}
		}
		return tokens;
	}

	private static boolean isWordBoundary(String line, int index) {
		if (index >= line.length()) {
			return true;
		}
		char c = line.charAt(index);
		return Character.isWhitespace(c) || c == ',' || c == ':' || c == ']' || c == '}';
	}

	static Line highlightLine(String lineText, String searchQuery, boolean isLineActiveMatch) {
		if (lineText.isEmpty()) {
			return Line.from(Collections.emptyList());
		}

		List<Token> tokens = tokenize(lineText);
		if (searchQuery == null || searchQuery.isEmpty()) {
			List<Span> spans = new ArrayList<>();
			for (Token token : tokens) {
				String text = lineText.substring(token.start, token.end);
				spans.add(Span.styled(text, token.style));
			}
			return Line.from(spans);
		}

		String lowerLine = lineText.toLowerCase(Locale.ROOT);
		String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
		if (!lowerLine.contains(lowerQuery)) {
			List<Span> spans = new ArrayList<>();
			for (Token token : tokens) {
				String text = lineText.substring(token.start, token.end);
				spans.add(Span.styled(text, token.style));
			}
			return Line.from(spans);
		}

		List<int[]> matchIntervals = new ArrayList<>();
		@Var int pos = 0;
		while (pos < lineText.length()) {
			int idx = lowerLine.indexOf(lowerQuery, pos);
			if (idx < 0) {
				break;
			}
			matchIntervals.add(new int[] { idx, idx + lowerQuery.length() });
			pos = idx + lowerQuery.length();
		}

		Style matchStyle = isLineActiveMatch
				? Style.EMPTY.bold().bg(Color.MAGENTA).fg(Color.WHITE)
				: Style.EMPTY.bold().bg(Color.YELLOW).fg(Color.BLACK);

		List<Span> spans = new ArrayList<>();
		for (Token token : tokens) {
			@Var int current = token.start;
			while (current < token.end) {
				@Var int[] containing = null;
				@Var int[] nextMatch = null;
				for (int[] m : matchIntervals) {
					if (current >= m[0] && current < m[1]) {
						containing = m;
						break;
					}
					if (m[0] > current) {
						if (nextMatch == null || m[0] < nextMatch[0]) {
							nextMatch = m;
						}
					}
				}

				if (containing != null) {
					int segEnd = Math.min(token.end, containing[1]);
					spans.add(Span.styled(lineText.substring(current, segEnd), matchStyle));
					current = segEnd;
				} else if (nextMatch != null && nextMatch[0] < token.end) {
					int segEnd = nextMatch[0];
					spans.add(Span.styled(lineText.substring(current, segEnd), token.style));
					current = segEnd;
				} else {
					spans.add(Span.styled(lineText.substring(current, token.end), token.style));
					current = token.end;
				}
			}
		}

		return Line.from(spans);
	}
}
