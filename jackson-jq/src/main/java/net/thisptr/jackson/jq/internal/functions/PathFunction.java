package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.RootPath;

@BuiltinFunction("path/1")
public class PathFunction implements Function {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public void apply(Scope scope, List<Expression> args, JsonNode in, Output output, Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, RootPath.getInstance(), (obj, path) -> {
			// `VALUE | path(VALUE) => []`
			if (path == null && in.isValueNode() && JsonNodeComparator.getInstance().compare(in, obj) == 0)
				path = RootPath.getInstance();
			if (path == null)
				throw new JsonQueryException("Invalid path expression with result %s", JsonNodeUtils.toString(obj));
			final ArrayNode out = MAPPER.createArrayNode();
			path.toJsonNode(out);
			output.emit(out);
		}, true);
	}
}
