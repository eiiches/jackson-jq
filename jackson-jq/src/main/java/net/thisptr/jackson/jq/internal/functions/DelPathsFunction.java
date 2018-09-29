package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.PathUtils;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.Path;

@BuiltinFunction("delpaths/1")
public class DelPathsFunction implements Function {

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (paths) -> {
			if (!paths.isArray()) {
				throw new JsonQueryException("Paths must be specified as an array");
			}

			final List<JsonNode> sortedPaths = new ArrayList<>(paths.size());
			for (final JsonNode path : paths) {
				if (!path.isArray())
					throw new JsonQueryException("Path must be specified as array, not " + path.getNodeType().toString().toLowerCase());
				sortedPaths.add(path);
			}
			sortedPaths.sort(JsonNodeComparator.getInstance());

			JsonNode out = in;
			for (int i = sortedPaths.size() - 1; i >= 0; --i) {
				final Path path = PathUtils.toPath(sortedPaths.get(i));
				out = path.mutate(out, (oldval) -> {
					if ((path instanceof ArrayRangeIndexPath) && oldval.isArray()) {
						final ArrayNode newval = scope.getObjectMapper().createArrayNode();
						for (int j = 0; j < oldval.size(); ++j)
							newval.add(MissingNode.getInstance());
						return newval;
					} else if ((path instanceof ArrayIndexPath) && ((ArrayIndexPath) path).index < 0 && version.compareTo(Versions.JQ_1_5) <= 0) {
						// jq-1.5: [1,2,[1,3]]|delpaths([[-1,1]]) #=> [1,2,[1]]
						// jq-1.5: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2,[1,3]]
						// jq-master: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2]
						return oldval;
					} else {
						return MissingNode.getInstance();
					}
				}, false);
			}

			output.emit(JsonNodeUtils.filter(out, (val) -> !val.isMissingNode()));
		});
	}
}
