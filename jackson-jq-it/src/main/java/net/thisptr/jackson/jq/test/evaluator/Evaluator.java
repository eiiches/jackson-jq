package net.thisptr.jackson.jq.test.evaluator;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Version;

public interface Evaluator {
	public static class Result {
		public final List<JsonNode> values;
		public final Throwable error;

		public Result(final List<JsonNode> values, final Throwable error) {
			this.values = values;
			this.error = error;
		}
	}

	Result evaluate(String expr, JsonNode in, Version version, long timeout) throws Throwable;
}