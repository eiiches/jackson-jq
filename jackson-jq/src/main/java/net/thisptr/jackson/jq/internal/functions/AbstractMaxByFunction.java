package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public abstract class AbstractMaxByFunction implements Function {
	protected static final JsonNodeComparator comparator = JsonNodeComparator.getInstance();

	private String fname;

	public AbstractMaxByFunction(final String fname) {
		this.fname = fname;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType(fname, in, JsonNodeType.ARRAY);

		JsonNode maxItem = NullNode.getInstance();
		JsonNode maxValue = null;
		for (final JsonNode i : in) {
			final ArrayNode value = scope.getObjectMapper().createArrayNode();
			args.get(0).apply(scope, i, value::add);
			if (maxValue == null || !isLarger(maxValue, value)) {
				maxValue = value;
				maxItem = i;
			}
		}

		output.emit(maxItem, null);
	}

	protected abstract boolean isLarger(final JsonNode criteria, final JsonNode value);
}
