package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.UnicodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
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
@FunctionRegistration(name = "length", nargs = 0)
public class LengthFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			output.emit(length(jsonProvider, in, version), UntrackedPath.getInstance());
		});
	}

	private <JsonNode> JsonNode length(JsonProvider<JsonNode> jsonProvider, JsonNode in, Version version) throws JsonQueryException {
		JsonNodeType type = jsonProvider.getNodeType(in);
		if (type == JsonNodeType.STRING) {
			return jsonProvider.createNumber(UnicodeUtils.lengthUtf32(jsonProvider.getString(in)));
		} else if (type == JsonNodeType.ARRAY) {
			return jsonProvider.createNumber(jsonProvider.getArrayLength(in));
		} else if (type == JsonNodeType.OBJECT) {
			return jsonProvider.createNumber(jsonProvider.getObjectMemberCount(in));
		} else if (type == JsonNodeType.NULL) {
			return jsonProvider.createNumber(0);
		} else if (type == JsonNodeType.NUMBER) {
			return JsonNodeUtils.asNumericNode(jsonProvider, Math.abs(jsonProvider.getNumberAsDoubleRounded(in)));
		} else {
			throw new JsonQueryTypeException("%s has no length", ExceptionMessages.describe(jsonProvider, version, in));
		}
	}
}
