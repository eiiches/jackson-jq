package net.thisptr.jackson.jq.v2.core.internal.builtins;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "keys_unsorted", nargs = 0)
public class KeysUnsortedFunction extends AbstractKeysFunction {
	public KeysUnsortedFunction() {
		super("keys_unsorted", false);
	}
}
