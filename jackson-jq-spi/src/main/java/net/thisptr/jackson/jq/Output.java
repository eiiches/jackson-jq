package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Output extends PathOutput {

	void emit(JsonNode out) throws JsonQueryException;

	@Override
	default void emit(final JsonNode out, final Path opath) throws JsonQueryException {
		emit(out);
	}
}