package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.internal.JsonNodeFunction;

@AutoService(Function.class)
@BuiltinFunction("infinite/0")
public class InfiniteFunction extends JsonNodeFunction {
	public InfiniteFunction() {
		super(DoubleNode.valueOf(Double.POSITIVE_INFINITY));
	}
}