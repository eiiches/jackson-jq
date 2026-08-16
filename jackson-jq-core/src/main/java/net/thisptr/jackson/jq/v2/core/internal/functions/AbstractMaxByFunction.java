package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;

public abstract class AbstractMaxByFunction implements FunctionFactory {

	private String fname;

	public AbstractMaxByFunction(String fname) {
		this.fname = fname;
	}

	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);

			@Var JsonNode maxItem = jsonProvider.createNull();
			@Var JsonNode maxValue = null;
			Iterator<JsonNode> iter = jsonProvider.elements(in);
			while (iter.hasNext()) {
				JsonNode i = iter.next();
				List<JsonNode> valueList = new ArrayList<>();
				args.get(0).apply(frame, i, valueList::add);
				JsonNode value = JsonNodeUtils.asArrayNode(jsonProvider, valueList);
				if (maxValue == null || !isLarger(jsonProvider, maxValue, value)) {
					maxValue = value;
					maxItem = i;
				}
			}

			output.emit(maxItem, null);
		};
	}

	protected abstract <JsonNode> boolean isLarger(JsonProvider<JsonNode> jsonProvider, JsonNode criteria, JsonNode value);
}
