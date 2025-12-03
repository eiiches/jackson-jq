package net.thisptr.jackson.jq.internal.functions;

import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
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
			for (final Entry<String, JsonNode> entry : in.properties()) {
				stack.push(new StringNode(entry.getKey()));
				applyRecursive(scope, entry.getValue(), output, stack, predicate);
				stack.pop();
			}
		}
	}
}
