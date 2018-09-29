package net.thisptr.jackson.jq;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Function {

	default void apply(Scope scope, List<Expression> args, JsonNode in, Output output, Version version) throws JsonQueryException {
		apply(scope, args, in, null, (out, path) -> {
			output.emit(out);
		}, version);
	}

	default void apply(Scope scope, List<Expression> args, JsonNode in, Path path, PathOutput output, Version version) throws JsonQueryException {
		apply(scope, args, in, (out) -> {
			output.emit(out, null);
		}, version);
	}
}
