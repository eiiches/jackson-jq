package net.thisptr.jackson.jq.v2.spi.exception;

/**
 * Reports that evaluation was stopped because it would have exceeded one of the budgets set by
 * {@code RuntimeOptions.setRuntimeLimits}.
 * <p>
 * Real jq has no equivalent error -- it allocates until the process dies -- so a query that runs
 * into this would have either succeeded or exhausted memory under jq itself.
 */
public class RuntimeLimitExceededException extends JsonQueryException {
	private static final long serialVersionUID = 6602284283096281130L;

	/**
	 * Creates an exception with the given message.
	 *
	 * @param msg the error message
	 */
	public RuntimeLimitExceededException(String msg) {
		super(msg);
	}
}
