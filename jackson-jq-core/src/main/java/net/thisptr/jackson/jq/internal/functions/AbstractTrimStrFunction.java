package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractTrimStrFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (trimText) -> {
			if (!in.isTextual() || !trimText.isTextual()) {
				output.emit(in, ipath);
				return;
			}
			final JsonNode out = TextNode.valueOf(doTrim(in.asText(), trimText.asText()));
			output.emit(out, null);
		});
	}

	protected abstract String doTrim(final String text, final String trim);
}
