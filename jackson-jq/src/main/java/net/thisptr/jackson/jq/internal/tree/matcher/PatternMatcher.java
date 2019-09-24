package net.thisptr.jackson.jq.internal.tree.matcher;

import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Functional;
import net.thisptr.jackson.jq.internal.misc.Pair;
import net.thisptr.jackson.jq.path.Path;

public interface PatternMatcher {

	void match(final Scope scope, final JsonNode in, final Functional.Consumer<List<Pair<String, JsonNode>>> out, final Stack<Pair<String, JsonNode>> accumulate, final boolean emit) throws JsonQueryException;

	public static class MatchWithPath {
		public final String name;
		public final JsonNode value;
		public final Path path;

		public MatchWithPath(final String name, final JsonNode value, final Path path) {
			this.name = name;
			this.value = value;
			this.path = path;
		}
	}

	public interface MatchOutput {
		void emit(List<MatchWithPath> vars) throws JsonQueryException;
	}

	void matchWithPath(Scope scope, JsonNode in, Path path, MatchOutput output, Stack<MatchWithPath> accumulate, boolean emit) throws JsonQueryException;
}
