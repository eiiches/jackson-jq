package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "rindex", nargs = 1)
public class RIndexFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression> args, Version version) {
		return (frame, in, ipath, output) -> {
			if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
				output.emit(jsonProvider.createNull(), null);
				return;
			}

			args.get(0).apply(jsonProvider, frame, in, (needle) -> {
				List<Integer> tmp = IndicesFunction.indices(jsonProvider, needle, in);
				if (tmp.isEmpty()) {
					output.emit(jsonProvider.createNull(), null);
				} else {
					output.emit(jsonProvider.createNumber(tmp.get(tmp.size() - 1)), null);
				}
			});
		};
	}
}
