package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.LegacyFunction;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration(name = "utf8bytelength", nargs = 0, version = @VersionRangeSpec(
		min = @VersionSpec(major = 1, minor = 6, patch = 0)
))
public class Utf8ByteLengthFunction implements LegacyFunction {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (jsonProvider.getNodeType(in) != JsonNodeType.STRING)
			throw new JsonQueryTypeException(jsonProvider, "%s only strings have UTF-8 byte length", in);
		output.emit(jsonProvider.createNumber(UnicodeUtils.lengthUtf8(jsonProvider.asText(in))), null);
	}
}
