package net.thisptr.jackson.jq.v2.core.internal.builtins;


import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "keys", nargs = 0)
public class KeysFunction extends AbstractKeysFunction {
	public KeysFunction() {
		super("keys", true);
	}
}
