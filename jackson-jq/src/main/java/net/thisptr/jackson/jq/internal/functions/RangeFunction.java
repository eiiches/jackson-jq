package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;

@BuiltinFunction("range/3")
public class RangeFunction implements Function {

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> beginTuple = args.get(0).apply(scope, in);
		final List<JsonNode> endTuple = args.get(1).apply(scope, in);
		final List<JsonNode> incTuple = args.get(2).apply(scope, in);

		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode beginNode : beginTuple) {
			for (final JsonNode endNode : endTuple) {
				for (final JsonNode incNode : incTuple) {
					if (!beginNode.isNumber() || !endNode.isNumber() || !incNode.isNumber())
						throw new JsonQueryException("arguments of range() must be NUMBER");
					final double begin = beginNode.asDouble();
					final double end = endNode.asDouble();
					final double inc = incNode.asDouble();

					if (inc > 0) {
						for (double i = begin; i < end; i += inc)
							out.add(JsonNodeUtils.asNumericNode(i));
					} else if (inc < 0) {
						for (double i = begin; i > end; i += inc)
							out.add(JsonNodeUtils.asNumericNode(i));
					}
				}
			}
		}

		return out;
	}
}
