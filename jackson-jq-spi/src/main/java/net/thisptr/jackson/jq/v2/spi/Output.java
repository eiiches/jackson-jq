package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public interface Output<JsonNode> extends PathOutput<JsonNode> {

	void emit(JsonNode out) throws JsonQueryException;

	@Override
	default void emit(final JsonNode out, final Path<JsonNode> opath) throws JsonQueryException {
		emit(out);
	}
}
