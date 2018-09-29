package net.thisptr.jackson.jq.extra.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function; import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction({ "timestamp/0" })
public class TimestampFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		output.emit(new LongNode(System.currentTimeMillis()));
	}
}