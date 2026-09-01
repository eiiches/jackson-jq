package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "has", nargs = 1)
public class HasFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType == JsonNodeType.NULL) {
				output.emit(jsonProvider.createBoolean(false), UntrackedPath.getInstance());
				return;
			}
			args.get(0).apply(frame, in, UntrackedPath.getInstance(), (keyName, opath) -> {
				JsonNodeType keyType = jsonProvider.getNodeType(keyName);
				if (inType == JsonNodeType.OBJECT) {
					if (keyType != JsonNodeType.STRING)
						throw new JsonQueryException("argument 1 of has() must be string for object input");
					output.emit(jsonProvider.createBoolean(jsonProvider.hasObjectMember(in, jsonProvider.getString(keyName))), UntrackedPath.getInstance());
				} else if (inType == JsonNodeType.ARRAY) {
					if (keyType != JsonNodeType.NUMBER)
						throw new JsonQueryException("argument 1 of has() must be int for array input");
					// NaN, the infinities and anything outside int range are all simply absent.
					Integer keyAsInt = jsonProvider.getNumberAsIntTruncated(keyName);
					boolean present = keyAsInt != null && keyAsInt >= 0 && keyAsInt < jsonProvider.getArrayLength(in);
					output.emit(jsonProvider.createBoolean(present), UntrackedPath.getInstance());
				} else {
					throw new JsonQueryException("has() is not applicable to " + inType);
				}
			});
		});
	}
}
