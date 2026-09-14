package net.thisptr.jackson.jq.v2.core.internal.exception;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class IllegalJsonArgumentException extends JsonQueryException {
	private static final long serialVersionUID = 1036641236398705267L;

	public IllegalJsonArgumentException(String msg) {
		super(msg);
	}
}
