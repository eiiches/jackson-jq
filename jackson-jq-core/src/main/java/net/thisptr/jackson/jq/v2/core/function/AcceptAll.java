package net.thisptr.jackson.jq.v2.core.function;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

/**
 * The singleton {@link FunctionFilter} that accepts every function.
 */
enum AcceptAll implements FunctionFilter {
	INSTANCE;

	@Override
	public boolean accept(FunctionSignature signature) {
		return true;
	}
}
