package net.thisptr.jackson.jq;

import java.util.List;

import net.thisptr.jackson.jq.exception.JsonQueryException;

import com.fasterxml.jackson.databind.JsonNode;

public interface Function {
	List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException;
}
