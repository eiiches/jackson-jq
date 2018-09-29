package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction("rindex/1")
public class RIndexFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (needle) -> {
			final List<Integer> tmp = IndicesFunction.indices(needle, in);
			if (tmp.isEmpty()) {
				output.emit(NullNode.getInstance());
			} else {
				output.emit(new IntNode(tmp.get(tmp.size() - 1)));
			}
		});
	}
}
