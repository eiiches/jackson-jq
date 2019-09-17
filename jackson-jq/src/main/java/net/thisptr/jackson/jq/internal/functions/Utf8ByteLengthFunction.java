package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction(value = "utf8bytelength/0", version = "[1.6, )")
public class Utf8ByteLengthFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (!in.isTextual())
			throw new JsonQueryTypeException("%s only strings have UTF-8 byte length", in);
		output.emit(IntNode.valueOf(UnicodeUtils.lengthUtf8(in.asText())), null);
	}
}
