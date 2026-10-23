package net.thisptr.jackson.jq.v2.core.function;

/**
 * The singleton {@link FunctionFilter} that accepts every function.
 */
enum AcceptAll implements FunctionFilter {
	INSTANCE;

	@Override
	public boolean accept(FunctionContext context) {
		return true;
	}
}
