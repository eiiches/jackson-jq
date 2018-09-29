package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("reverse/0")
public class ReverseFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("reverse", in, JsonNodeType.ARRAY);

		final List<JsonNode> items = JsonNodeUtils.asArrayList((ArrayNode) in);
		Collections.reverse(items);
		output.emit(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), items));
	}
}
