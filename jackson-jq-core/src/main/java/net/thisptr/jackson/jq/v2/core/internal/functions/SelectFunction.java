package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "select", nargs = 1)
public class SelectFunction implements FunctionFactory {
	@Override
	public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
		Expression pred = args.get(0);
		return (scope, in, path, output) -> {
			pred.apply(scope, in, path, (val, p) -> {
				if (JsonNodeUtils.asBoolean(jsonProvider, val)) {
					output.emit(in, path);
				}
			}, false);
		};
	}
}
