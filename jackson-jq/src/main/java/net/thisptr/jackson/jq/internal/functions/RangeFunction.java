package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.JsonQueryUtils;

@BuiltinFunction("range/3")
public class RangeFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		final List<JsonNode> beginTuple = JsonQueryUtils.applyToArrayList(args.get(0), scope, in);
		final List<JsonNode> endTuple = JsonQueryUtils.applyToArrayList(args.get(1), scope, in);
		final List<JsonNode> incTuple = JsonQueryUtils.applyToArrayList(args.get(2), scope, in);

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
							output.emit(JsonNodeUtils.asNumericNode(i));
					} else if (inc < 0) {
						for (double i = begin; i > end; i += inc)
							output.emit(JsonNodeUtils.asNumericNode(i));
					}
				}
			}
		}
	}
}
