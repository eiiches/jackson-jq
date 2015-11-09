package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;

public abstract class AbstractMaxFunction implements Function {
	protected static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	private String fname;

	public AbstractMaxFunction(final String fname) {
		this.fname = fname;
	}

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType(fname, in, JsonNodeType.ARRAY);

		JsonNode maxItem = NullNode.getInstance();
		JsonNode maxValue = null;
		for (final JsonNode i : in) {
			final JsonNode value = JsonNodeUtils.asArrayNode(scope.getObjectMapper(), args.get(0).apply(scope, i));
			if (maxValue == null || !isLarger(maxValue, value)) {
				maxValue = value;
				maxItem = i;
			}
		}
		return Collections.singletonList(maxItem);
	}

	protected abstract boolean isLarger(final JsonNode criteria, final JsonNode value);
}
