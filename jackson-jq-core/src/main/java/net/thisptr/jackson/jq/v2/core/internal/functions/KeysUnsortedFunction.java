package net.thisptr.jackson.jq.v2.core.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration("keys_unsorted/0")
public class KeysUnsortedFunction extends AbstractKeysFunction {
	public KeysUnsortedFunction() {
		super("keys_unsorted", false);
	}
}
