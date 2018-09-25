package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public abstract class AbstractStartsEndsWithFunction implements Function {
	private final String fname;

	public AbstractStartsEndsWithFunction(final String fname) {
		this.fname = fname;
	}

	protected abstract boolean doCheck(final String text, final String needle);

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (needle) -> {
			if (!needle.isTextual() || !in.isTextual())
				throw new JsonQueryException(fname + "() requires string inputs");
			output.emit(BooleanNode.valueOf(doCheck(in.asText(), needle.asText())));
		});
	}
}
