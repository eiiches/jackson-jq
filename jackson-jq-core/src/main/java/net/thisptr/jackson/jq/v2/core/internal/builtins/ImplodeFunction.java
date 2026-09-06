package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.Iterator;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "implode", nargs = 0)
public class ImplodeFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {

			Preconditions.checkInputArrayType(jsonProvider, "implode", in, JsonNodeType.NUMBER);

			StringBuilder builder = new StringBuilder();
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
			while (iter.hasNext()) {
				JsonNode ch = iter.next();
				Integer codepoint = jsonProvider.getNumberAsIntTruncated(ch);
				if (codepoint == null) // NaN, an infinity, or beyond int range.
					throw new JsonQueryException("Cannot use " + jsonProvider.format(ch) + " as a unicode codepoint");
				builder.append((char) codepoint.intValue());
			}

			output.emit(jsonProvider.createString(builder.toString()), UntrackedPath.getInstance());
		});
	}
}
