package net.thisptr.jackson.jq.v2.test.evaluator;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface Evaluator {
	enum ErrorPhase {
		COMPILE,
		RUNTIME
	}

	record Result(List<JsonNode> values, @Nullable Throwable error, @Nullable ErrorPhase errorPhase,
				  @Nullable String stderr) {
	}

	Result evaluate(String expr, JsonNode in, Duration timeout) throws Throwable;
}
