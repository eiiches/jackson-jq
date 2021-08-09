package net.thisptr.jackson.jq.internal.functions.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonQueryJacksonModule;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("debug_scope/0")
public class DebugScopeFunction implements Function {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.registerModule(JsonQueryJacksonModule.getInstance());

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		final Map<String, Object> info = new HashMap<>();
		info.put("scope", scope);
		info.put("input", in);
		output.emit(MAPPER.valueToTree(info), null);
	}
}
