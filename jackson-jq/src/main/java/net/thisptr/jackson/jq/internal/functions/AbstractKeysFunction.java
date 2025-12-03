package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.StringNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Lists;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public class AbstractKeysFunction implements Function {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(final String name, final boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType(name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

		if (in.isObject()) {
			final List<String> keys = Lists.newArrayList(in.propertyNames());
			if (sortKeys)
				Collections.sort(keys);

			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (final String key : keys)
				result.add(new StringNode(key));
			output.emit(result, null);
		} else if (in.isArray()) {
			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (int i = 0; i < in.size(); ++i)
				result.add(new IntNode(i));
			output.emit(result, null);
		} else {
			throw new IllegalStateException();
		}
	}
}
