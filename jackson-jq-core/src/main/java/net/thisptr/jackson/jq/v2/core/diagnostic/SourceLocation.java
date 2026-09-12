package net.thisptr.jackson.jq.v2.core.diagnostic;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A half-open region of jq source text, in 1-based lines and columns.
 * <p>
 * The end position is inclusive of the last character, matching what the parser's tokens report:
 * a single-character token at the start of a one-line query is
 * {@code (1, 1)}-{@code (1, 1)}, not {@code (1, 1)}-{@code (1, 2)}.
 */
public final class SourceLocation {
	private final int beginLine;
	private final int beginColumn;
	private final int endLine;
	private final int endColumn;

	private SourceLocation(int beginLine, int beginColumn, int endLine, int endColumn) {
		this.beginLine = beginLine;
		this.beginColumn = beginColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	/**
	 * Creates a location spanning from one position to another.
	 *
	 * @param beginLine the 1-based line of the first character
	 * @param beginColumn the 1-based column of the first character
	 * @param endLine the 1-based line of the last character
	 * @param endColumn the 1-based column of the last character
	 * @return the location
	 * @throws IllegalArgumentException if any coordinate is less than 1
	 */
	public static SourceLocation of(int beginLine, int beginColumn, int endLine, int endColumn) {
		requirePositive(beginLine, "beginLine");
		requirePositive(beginColumn, "beginColumn");
		requirePositive(endLine, "endLine");
		requirePositive(endColumn, "endColumn");
		return new SourceLocation(beginLine, beginColumn, endLine, endColumn);
	}

	/**
	 * Creates a location covering a single character.
	 *
	 * @param line the 1-based line
	 * @param column the 1-based column
	 * @return the location
	 * @throws IllegalArgumentException if either coordinate is less than 1
	 */
	public static SourceLocation of(int line, int column) {
		return of(line, column, line, column);
	}

	private static void requirePositive(int value, String name) {
		if (value < 1)
			throw new IllegalArgumentException(name + " must be 1 or greater, but was " + value);
	}

	/**
	 * Returns the smallest location covering both arguments -- used by productions that span
	 * several children.
	 *
	 * @param begin the location of the first child
	 * @param end the location of the last child
	 * @return the combined location
	 */
	public static SourceLocation span(SourceLocation begin, SourceLocation end) {
		Objects.requireNonNull(begin, "begin");
		Objects.requireNonNull(end, "end");
		return new SourceLocation(begin.beginLine, begin.beginColumn, end.endLine, end.endColumn);
	}

	/**
	 * Returns the 1-based line of the first character.
	 *
	 * @return the begin line
	 */
	public int beginLine() {
		return beginLine;
	}

	/**
	 * Returns the 1-based column of the first character.
	 *
	 * @return the begin column
	 */
	public int beginColumn() {
		return beginColumn;
	}

	/**
	 * Returns the 1-based line of the last character.
	 *
	 * @return the end line
	 */
	public int endLine() {
		return endLine;
	}

	/**
	 * Returns the 1-based column of the last character.
	 *
	 * @return the end column
	 */
	public int endColumn() {
		return endColumn;
	}

	/**
	 * Renders the line this location begins on, followed by a caret line marking the begin column,
	 * in the style jq itself uses to report syntax errors. Both lines are indented by four spaces
	 * and separated by a newline; there is no trailing newline.
	 * <p>
	 * Returns {@code null} if {@code source} does not have a {@link #beginLine()}.
	 *
	 * @param source the jq source text this location refers to
	 * @return the excerpt, or {@code null} if the line is not present in {@code source}
	 */
	public @Nullable String excerpt(String source) {
		Objects.requireNonNull(source, "source");
		String[] lines = source.split("\r\n|\r|\n", -1);
		if (beginLine > lines.length)
			return null;
		String line = lines[beginLine - 1];
		StringBuilder caret = new StringBuilder();
		for (int i = 0; i < beginColumn - 1; ++i)
			caret.append(i < line.length() && line.charAt(i) == '\t' ? '\t' : ' ');
		caret.append('^');
		return "    " + line + "\n    " + caret;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof SourceLocation))
			return false;
		SourceLocation that = (SourceLocation) o;
		return beginLine == that.beginLine && beginColumn == that.beginColumn
				&& endLine == that.endLine && endColumn == that.endColumn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(beginLine, beginColumn, endLine, endColumn);
	}

	@Override
	public String toString() {
		return "line " + beginLine + ", column " + beginColumn;
	}
}
