package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Lists;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

public class AbstractKeysFunction implements Function {
	private final boolean sortKeys;
	private final String name;

	public AbstractKeysFunction(final String name, final boolean sortKeys) {
		this.name = name;
		this.sortKeys = sortKeys;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType(name, in, JsonNodeType.OBJECT, JsonNodeType.ARRAY);

		if (in.isObject()) {
			final List<String> keys = Lists.newArrayList(in.fieldNames());
			if (sortKeys)
				Collections.sort(keys);

			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (final String key : keys)
				result.add(new TextNode(key));
			output.emit(result);
		} else if (in.isArray()) {
			final ArrayNode result = scope.getObjectMapper().createArrayNode();
			for (int i = 0; i < in.size(); ++i)
				result.add(new IntNode(i));
			output.emit(result);
		} else {
			throw new IllegalStateException();
		}
	}
}
