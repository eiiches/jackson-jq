package net.thisptr.jackson.jq;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface Function {

	void apply(Scope scope, List<Expression> args, JsonNode in, Output output) throws JsonQueryException;
}
