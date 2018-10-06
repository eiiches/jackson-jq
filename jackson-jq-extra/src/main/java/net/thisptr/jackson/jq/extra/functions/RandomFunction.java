package net.thisptr.jackson.jq.extra.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.path.Path;

@BuiltinFunction("random/0")
public class RandomFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		output.emit(new DoubleNode(Math.random()), null);
	}
}
