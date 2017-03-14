package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.node.DoubleNode;

import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonNodeFunction;

@BuiltinFunction("nan/0")
public class NanFunction extends JsonNodeFunction {
	public NanFunction() {
		super(DoubleNode.valueOf(Double.NaN));
	}
}