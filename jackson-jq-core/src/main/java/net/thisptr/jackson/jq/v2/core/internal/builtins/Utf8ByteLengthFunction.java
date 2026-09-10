package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.commons.strings.UnicodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.exception.ExceptionMessages;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "utf8bytelength", nargs = 0, version = @VersionRangeSpec(
		min = @VersionSpec(major = 1, minor = 6, patch = 0)
))
public class Utf8ByteLengthFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {

			if (!jsonProvider.isString(in))
				throw new JsonQueryTypeException("%s only strings have UTF-8 byte length", ExceptionMessages.describe(jsonProvider, version, in));
			output.emit(jsonProvider.createNumber(UnicodeUtils.lengthUtf8(jsonProvider.getString(in))), UntrackedPath.getInstance());
		});
	}
}
