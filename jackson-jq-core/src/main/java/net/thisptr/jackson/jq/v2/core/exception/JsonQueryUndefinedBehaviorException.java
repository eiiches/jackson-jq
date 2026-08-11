package net.thisptr.jackson.jq.v2.core.exception;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryUndefinedBehaviorException extends JsonQueryException {
	private static final long serialVersionUID = 6910999258451981582L;

	public JsonQueryUndefinedBehaviorException(final String msg) {
		super(msg);
	}
}
