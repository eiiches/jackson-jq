package net.thisptr.jackson.jq.v2.core.internal.exception;

import java.io.Serial;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IllegalJsonArgumentException extends JsonQueryException {
	@Serial
	private static final long serialVersionUID = 1036641236398705267L;

	public IllegalJsonArgumentException(String msg) {
		super(msg);
	}
}
