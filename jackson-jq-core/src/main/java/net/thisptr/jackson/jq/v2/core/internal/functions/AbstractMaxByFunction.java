package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class AbstractMaxByFunction implements Function {

	private String fname;

	public AbstractMaxByFunction(String fname) {
		this.fname = fname;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);

		@Var JsonNode maxItem = jsonProvider.createNull();
		@Var JsonNode maxValue = null;
		Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			JsonNode i = iter.next();
			List<JsonNode> valueList = new ArrayList<>();
			args.get(0).apply(scope, i, valueList::add);
			JsonNode value = JsonNodeUtils.asArrayNode(jsonProvider, valueList);
			if (maxValue == null || !isLarger(jsonProvider, maxValue, value)) {
				maxValue = value;
				maxItem = i;
			}
		}

		output.emit(maxItem, null);
	}

	protected abstract <JsonNode> boolean isLarger(JsonProvider<JsonNode> jsonProvider, JsonNode criteria, JsonNode value);
}
