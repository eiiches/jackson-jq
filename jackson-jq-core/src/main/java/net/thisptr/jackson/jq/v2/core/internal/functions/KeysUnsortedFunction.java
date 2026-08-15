package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "keys_unsorted", nargs = 0)
public class KeysUnsortedFunction extends AbstractKeysFunction {
	public KeysUnsortedFunction() {
		super("keys_unsorted", false);
	}
}
