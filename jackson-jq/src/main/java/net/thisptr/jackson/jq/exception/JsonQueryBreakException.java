package net.thisptr.jackson.jq.exception;

public class JsonQueryBreakException extends JsonQueryException {
	private static final long serialVersionUID = -6066878919494380889L;

	public JsonQueryBreakException() {
		super("break");
	}
}
