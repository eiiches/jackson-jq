package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

// For security reasons, env/0 should not be loaded by default.
// @AutoService(Function.class)
// 2022-06-29(eiiches): commented out @BuiltinFunction("env/0") to make sure some custom function loaders don't load `env/0` accidentally.
// @BuiltinFunction("env/0")
public class EnvFunction implements Function {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		output.emit(MAPPER.valueToTree(System.getenv()), null);
	}
}
