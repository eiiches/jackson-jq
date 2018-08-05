package net.thisptr.jackson.jq.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonQueryException extends JsonProcessingException {
	private static final long serialVersionUID = -7241258446595502920L;

	public JsonQueryException(final String msg) {
		super(msg);
	}

	public JsonQueryException(final Throwable e) {
		super(e);
	}

	public JsonQueryException(final String msg, final Throwable rootCause) {
		super(msg, rootCause);
	}

	public JsonNode getMessageAsJsonNode() {
		return new TextNode(getMessage());
	}

	public static JsonQueryException format(String format, Object... args) {
		final Object[] args_ = new Object[args.length];
		for (int i = 0; i < args.length; ++i) {
			if (args[i] instanceof JsonNodeType) {
				args_[i] = args[i].toString().toLowerCase();
				continue;
			}
			if (args[i] instanceof Double) {
				final double val = ((Double) args[i]).doubleValue();
				if (val == (long) val) {
					args_[i] = Long.valueOf((long) val);
				} else {
					args_[i] = val;
				}
				continue;
			}
			args_[i] = args[i];
		}
		return new JsonQueryException(String.format(format, args_));
	}
}
