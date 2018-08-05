package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class ThisObject implements Expression {
	@Override
	public void apply(Scope scope, JsonNode in, final Output output) throws JsonQueryException {
		output.emit(in);
	}

	@Override
	public String toString() {
		return ".";
	}
}
