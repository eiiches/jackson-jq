package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

@BuiltinFunction("paths/1")
public class PathsFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final Stack<JsonNode> stack = new Stack<>();
		final List<JsonNode> out = new ArrayList<>();
		applyRecursive(scope, in, out, stack, args.get(0));
		return out;
	}

	private void applyRecursive(final Scope scope, final JsonNode in, final List<JsonNode> out, final Stack<JsonNode> stack, final JsonQuery predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			for (final JsonNode shouldInclude : predicate.apply(in))
				if (JsonNodeUtils.asBoolean(shouldInclude))
					out.add(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), stack));
		}

		if (in.isArray()) {
			for (int i = 0; i < in.size(); ++i) {
				stack.push(new IntNode(i));
				applyRecursive(scope, in.get(i), out, stack, predicate);
				stack.pop();
			}
		} else if (in.isObject()) {
			final Iterator<Entry<String, JsonNode>> iter = in.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> i = iter.next();
				stack.push(new TextNode(i.getKey()));
				applyRecursive(scope, i.getValue(), out, stack, predicate);
				stack.pop();
			}
		}
	}
}
