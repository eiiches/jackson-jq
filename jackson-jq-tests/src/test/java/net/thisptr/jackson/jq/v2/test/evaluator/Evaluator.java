package net.thisptr.jackson.jq.v2.test.evaluator;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface Evaluator {
	class Result {
		public List<JsonNode> values;
		public @Nullable Throwable error;

		public Result(List<JsonNode> values, @Nullable Throwable error) {
			this.values = values;
			this.error = error;
		}
	}

	Result evaluate(String expr, JsonNode in, Duration timeout) throws Throwable;
}
