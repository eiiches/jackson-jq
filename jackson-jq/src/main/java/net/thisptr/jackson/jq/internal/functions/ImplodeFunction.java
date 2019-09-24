package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("implode/0")
public class ImplodeFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputArrayType("implode", in, JsonNodeType.NUMBER);

		final StringBuilder builder = new StringBuilder();
		for (final JsonNode ch : in) {
			if (ch.canConvertToInt()) {
				builder.append((char) ch.asInt());
			} else {
				throw new JsonQueryException("input to implode() must be a list of codepoints; " + ch.getNodeType() + " found");
			}
		}

		output.emit(new TextNode(builder.toString()), null);
	}
}
