package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "map", nargs = 1)
public class MapFunction implements FunctionFactory {
	@Override
	public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
		Expression f = args.get(0);
		return (frame, in, path, output) -> {
			if (jsonProvider.getNodeType(in) != JsonNodeType.ARRAY) {
				throw new JsonQueryException("Cannot map over " + JsonNodeUtils.typeOf(jsonProvider, in));
			}
			N outArr = jsonProvider.createArray();
			int size = jsonProvider.size(in);
			for (int i = 0; i < size; i++) {
				N rawElem = jsonProvider.get(in, i);
				N elem = rawElem != null ? rawElem : jsonProvider.createNull();
				f.apply(jsonProvider, frame, elem, null, (res, p) -> {
					jsonProvider.add(outArr, res);
				}, false);
			}
			output.emit(outArr, null);
		};
	}
}
