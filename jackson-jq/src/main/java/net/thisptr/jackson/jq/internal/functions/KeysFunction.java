package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;

@AutoService(Function.class)
@BuiltinFunction("keys/0")
public class KeysFunction extends AbstractKeysFunction {
	public KeysFunction() {
		super("keys", true);
	}
}
