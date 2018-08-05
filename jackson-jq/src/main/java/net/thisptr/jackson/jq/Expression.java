package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface Expression {

	void apply(Scope scope, JsonNode in, Output output) throws JsonQueryException;
}