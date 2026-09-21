package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.io.Serial;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * Indicates that strict compile-time type checking found one or more errors.
 */
public final class TypeCheckException extends JsonQueryException {
	@Serial
	private static final long serialVersionUID = 1L;

	TypeCheckException(int errors) {
		super("Type checking failed with " + errors + (errors == 1 ? " error" : " errors"));
	}
}
