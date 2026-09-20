package net.thisptr.jackson.jq.v2.core.internal.exception;

import java.io.Serial;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class JsonQueryUndefinedBehaviorException extends JsonQueryException {
	@Serial
	private static final long serialVersionUID = 6910999258451981582L;

	public JsonQueryUndefinedBehaviorException(String msg) {
		super(msg);
	}
}
