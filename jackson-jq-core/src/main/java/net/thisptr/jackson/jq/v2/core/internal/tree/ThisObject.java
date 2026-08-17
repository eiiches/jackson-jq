package net.thisptr.jackson.jq.v2.core.internal.tree;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ThisObject<JsonNode> implements Expression<JsonNode> {

	@Override
	public String toString() {
		return ".";
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		output.emit(in, ipath);
	}
}
