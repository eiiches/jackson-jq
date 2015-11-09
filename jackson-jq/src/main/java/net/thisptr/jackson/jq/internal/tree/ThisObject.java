package net.thisptr.jackson.jq.internal.tree;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;

import com.fasterxml.jackson.databind.JsonNode;

public class ThisObject extends JsonQuery {
	@Override
	public List<JsonNode> apply(Scope scope, JsonNode in) {
		return Collections.singletonList(in);
	}

	@Override
	public String toString() {
		return ".";
	}
}
