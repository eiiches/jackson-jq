package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

@BuiltinFunction("paths/1")
public class PathsFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		final Stack<JsonNode> stack = new Stack<>();
		applyRecursive(scope, in, output, stack, args.get(0));
	}

	private void applyRecursive(final Scope scope, final JsonNode in, final PathOutput output, final Stack<JsonNode> stack, final Expression predicate) throws JsonQueryException {
		if (!stack.isEmpty()) {
			predicate.apply(scope, in, (shouldInclude) -> {
				if (JsonNodeUtils.asBoolean(shouldInclude))
					output.emit(JsonNodeUtils.asArrayNode(scope.getObjectMapper(), stack), null);
			});
		}

		if (in.isArray()) {
			for (int i = 0; i < in.size(); ++i) {
				stack.push(new IntNode(i));
				applyRecursive(scope, in.get(i), output, stack, predicate);
				stack.pop();
			}
		} else if (in.isObject()) {
			final Iterator<Entry<String, JsonNode>> iter = in.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> i = iter.next();
				stack.push(new TextNode(i.getKey()));
				applyRecursive(scope, i.getValue(), output, stack, predicate);
				stack.pop();
			}
		}
	}
}
