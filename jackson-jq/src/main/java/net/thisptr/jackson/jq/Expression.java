package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Expression {

	@Deprecated
	default void apply(Scope scope, JsonNode in, Output output) throws JsonQueryException {
		apply(scope, in, null, (out, path) -> {
			output.emit(out);
		}, false);
	}

	default void apply(Scope scope, JsonNode in, Path ipath, PathOutput output, boolean requirePath) throws JsonQueryException {
		apply(scope, in, (out) -> {
			output.emit(out, null);
		});
	}
}
