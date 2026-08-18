package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "keys", nargs = 0)
public class KeysFunction extends AbstractKeysFunction {
	public KeysFunction() {
		super("keys", true);
	}
}
