package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;

public interface Output {

	void emit(JsonNode out) throws JsonQueryException;
}