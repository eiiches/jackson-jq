package net.thisptr.jackson.jq.exception;

public class IllegalJsonArgumentException extends JsonQueryException {
	private static final long serialVersionUID = 1036641236398705267L;

	public IllegalJsonArgumentException(String msg) {
		super(msg);
	}
}
