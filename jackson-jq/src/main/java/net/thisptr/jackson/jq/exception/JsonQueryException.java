package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class JsonQueryException extends JsonProcessingException {
	private static final long serialVersionUID = -7241258446595502920L;

	public JsonQueryException(final String msg) {
		super(msg);
	}

	public JsonQueryException(final Throwable e) {
		super(e);
	}

	public static JsonQueryException format(String format, Object... args) {
		final Object[] args2 = args.clone();
		for (int i = 0; i < args2.length; ++i) {
			if (args2[i] instanceof JsonNodeType)
				args2[i] = args2[i].toString().toLowerCase();
		}
		return new JsonQueryException(String.format(format, args2));
	}
}
