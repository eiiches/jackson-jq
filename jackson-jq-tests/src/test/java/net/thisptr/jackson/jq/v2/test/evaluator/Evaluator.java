package net.thisptr.jackson.jq.v2.test.evaluator;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.spi.Version;

public interface Evaluator {
	class Result {
		public List<JsonNode> values;
		public Throwable error;

		public Result(List<JsonNode> values, Throwable error) {
			this.values = values;
			this.error = error;
		}
	}

	Result evaluate(String expr, JsonNode in, Version version, long timeout) throws Throwable;
}
