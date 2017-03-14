package net.thisptr.jackson.jq.internal.tree.matcher;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;

public interface PatternMatcher {
	void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException;
}