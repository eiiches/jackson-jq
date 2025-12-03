package net.thisptr.jackson.jq.extra.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.StringNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("uridecode/0")
public class UriDecodeFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("urldecode", in, JsonNodeType.STRING);

		try {
			output.emit(new StringNode(URLDecoder.decode(in.asString(), "UTF-8")), null);
		} catch (UnsupportedEncodingException e) {
			throw new JsonQueryException(e);
		}
	}
}
