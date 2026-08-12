package net.thisptr.jackson.jq.v2.test.evaluator;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Version;

public interface Evaluator {
	class Result {
		public List<JsonNode> values;
		public @Nullable Throwable error;

		public Result(List<JsonNode> values, @Nullable Throwable error) {
			this.values = values;
			this.error = error;
		}
	}

	Result evaluate(String expr, JsonNode in, Version version, long timeout) throws Throwable;
}
