package net.thisptr.jackson.jq.internal;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public class JsonNodeFunction implements Function {
	private JsonNode value;

	public JsonNodeFunction(final JsonNode value) {
		this.value = value;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		output.emit(value, null);
	}
}
