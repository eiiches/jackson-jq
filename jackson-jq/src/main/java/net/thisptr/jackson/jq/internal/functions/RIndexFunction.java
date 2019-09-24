package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("rindex/1")
public class RIndexFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (in.isNull()) {
			output.emit(NullNode.getInstance(), null);
			return;
		}

		args.get(0).apply(scope, in, (needle) -> {
			final List<Integer> tmp = IndicesFunction.indices(needle, in);
			if (tmp.isEmpty()) {
				output.emit(NullNode.getInstance(), null);
			} else {
				output.emit(new IntNode(tmp.get(tmp.size() - 1)), null);
			}
		});
	}
}
