package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.JsonNodeFunction;

@AutoService(Function.class)
@BuiltinFunction("nan/0")
public class NanFunction extends JsonNodeFunction {
	public NanFunction() {
		super(DoubleNode.valueOf(Double.NaN));
	}
}