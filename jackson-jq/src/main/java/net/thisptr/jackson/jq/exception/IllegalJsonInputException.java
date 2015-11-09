package net.thisptr.jackson.jq.exception;

public class IllegalJsonInputException extends JsonQueryException {
	private static final long serialVersionUID = -3734135414103466554L;

	public IllegalJsonInputException(final String msg) {
		super(msg);
	}
}
