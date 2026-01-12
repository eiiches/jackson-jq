package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractMaxByFunction<JsonNode> implements Function<JsonNode> {

	private String fname;

	public AbstractMaxByFunction(final String fname) {
		this.fname = fname;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);

		JsonNode maxItem = jsonProvider.createNull();
		JsonNode maxValue = null;
		final Iterator<JsonNode> iter = jsonProvider.elements(in);
		while (iter.hasNext()) {
			final JsonNode i = iter.next();
			final List<JsonNode> valueList = new ArrayList<>();
			args.get(0).apply(scope, i, valueList::add);
			final JsonNode value = JsonNodeUtils.asArrayNode(jsonProvider, valueList);
			if (maxValue == null || !isLarger(jsonProvider, maxValue, value)) {
				maxValue = value;
				maxItem = i;
			}
		}

		output.emit(maxItem, null);
	}

	protected abstract boolean isLarger(final JsonProvider<JsonNode> jsonProvider, final JsonNode criteria, final JsonNode value);
}
