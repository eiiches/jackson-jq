package com.intellij.ide.todo;

/**
 * Stand-in for the real IntelliJ platform class, which is absent from the shaded
 * intellij-code-formatter jar. FormatCommentsProcessor needs it transitively to detect
 * multi-line TODO comment continuations; returning false from isMultiLine() simply skips that
 * grouping, which doesn't affect normal Javadoc formatting.
 */
public final class TodoConfiguration {
	private static final TodoConfiguration INSTANCE = new TodoConfiguration();

	public static TodoConfiguration getInstance() {
		return INSTANCE;
	}

	public boolean isMultiLine() {
		return false;
	}
}
