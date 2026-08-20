package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

// TODO: remove or move to core
public interface Output<JsonNode> extends PathOutput<JsonNode> {

	void emit(JsonNode out) throws JsonQueryException;

	@Override
	default void emit(JsonNode out, @Nullable Path<JsonNode> opath) throws JsonQueryException {
		emit(out);
	}
}
