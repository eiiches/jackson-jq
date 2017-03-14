package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.node.DoubleNode;

import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.JsonNodeFunction;

@BuiltinFunction("infinite/0")
public class InfiniteFunction extends JsonNodeFunction {
	public InfiniteFunction() {
		super(DoubleNode.valueOf(Double.POSITIVE_INFINITY));
	}
}