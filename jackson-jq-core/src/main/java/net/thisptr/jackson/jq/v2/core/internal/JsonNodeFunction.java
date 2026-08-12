package net.thisptr.jackson.jq.v2.core.internal;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class JsonNodeFunction<JsonNode> implements Function {
	private JsonNode value;

	public JsonNodeFunction(final JsonNode value) {
		this.value = value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <InputNode> void apply(final Scope<InputNode> scope, final List<Expression<InputNode>> args, final InputNode in, final Path<InputNode> ipath, final PathOutput<InputNode> output, final Version version) throws JsonQueryException {
		output.emit((InputNode) value, null);
	}
}
