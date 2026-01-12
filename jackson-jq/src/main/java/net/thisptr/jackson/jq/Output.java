package net.thisptr.jackson.jq;

import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public interface Output<JsonNode> extends PathOutput<JsonNode> {

	void emit(JsonNode out) throws JsonQueryException;

	@Override
	default void emit(final JsonNode out, final Path<JsonNode> opath) throws JsonQueryException {
		emit(out);
	}
}
