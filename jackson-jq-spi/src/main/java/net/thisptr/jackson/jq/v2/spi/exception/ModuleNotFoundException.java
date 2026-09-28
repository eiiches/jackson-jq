package net.thisptr.jackson.jq.v2.spi.exception;

import java.io.Serial;

/**
 * Reports that a module or module loader could not resolve an imported path.
 * <p>
 * This is the "I don't have it" answer, as opposed to "I have it but reading it failed": a
 * provider that found the module and then failed to read it throws a plain
 * {@link JsonQueryException} instead. The compiler relies on this distinction when trying the
 * importing module and then the environment's loaders.
 */
public class ModuleNotFoundException extends JsonQueryException {
	@Serial
	private static final long serialVersionUID = 1L;

	private final String path;

	/**
	 * Creates an exception for the given import path.
	 *
	 * @param path the import path that could not be resolved
	 */
	public ModuleNotFoundException(String path) {
		super("module not found: " + path);
		this.path = path;
	}

	/**
	 * Creates an exception for the given import path, wrapping the given cause.
	 *
	 * @param path the import path that could not be resolved
	 * @param cause the underlying cause
	 */
	public ModuleNotFoundException(String path, Throwable cause) {
		super("module not found: " + path, cause);
		this.path = path;
	}

	/**
	 * Returns the import path that could not be resolved.
	 *
	 * @return the import path, as written in the {@code import} or {@code include} statement
	 */
	public String getPath() {
		return path;
	}
}
