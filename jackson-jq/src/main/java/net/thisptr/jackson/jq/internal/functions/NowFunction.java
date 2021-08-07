package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
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
@BuiltinFunction("now/0")
public class NowFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output, final Version version) throws JsonQueryException {
		output.emit(new DoubleNode(System.currentTimeMillis() / 1000.0), null);
	}
}
