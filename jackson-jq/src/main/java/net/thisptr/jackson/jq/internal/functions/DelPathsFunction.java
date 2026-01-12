package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.internal.misc.PathUtils;
import net.thisptr.jackson.jq.path.ArrayIndexPath;
import net.thisptr.jackson.jq.path.ArrayRangeIndexPath;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("delpaths/1")
public class DelPathsFunction<JsonNode> implements Function<JsonNode> {

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (paths) -> {
			if (jsonProvider.getNodeType(paths) != JsonNodeType.ARRAY) {
				throw new JsonQueryException("Paths must be specified as an array");
			}

			final List<JsonNode> sortedPaths = new ArrayList<>(jsonProvider.size(paths));
			for (final JsonNode path : jsonProvider.iterate(paths)) {
				if (jsonProvider.getNodeType(path) != JsonNodeType.ARRAY)
					throw new JsonQueryException("Path must be specified as array, not " + JsonNodeUtils.typeOf(jsonProvider, path));
				sortedPaths.add(path);
			}
			sortedPaths.sort(new JsonNodeComparator<>(jsonProvider));

			JsonNode out = in;
			for (int i = sortedPaths.size() - 1; i >= 0; --i) {
				final Path<JsonNode> path = PathUtils.toPath(jsonProvider, sortedPaths.get(i));
				out = path.mutate(jsonProvider, out, (oldval) -> {
					if ((path instanceof ArrayRangeIndexPath) && jsonProvider.getNodeType(oldval) == JsonNodeType.ARRAY) {
						JsonNode newval = jsonProvider.createArray();
						for (int j = 0; j < jsonProvider.size(oldval); ++j)
							newval = jsonProvider.add(newval, jsonProvider.createMissing());
						return newval;
					} else if ((path instanceof ArrayIndexPath) && jsonProvider.asDouble(((ArrayIndexPath<JsonNode>) path).index) < 0 && version.compareTo(Versions.JQ_1_5) <= 0) {
						// jq-1.5: [1,2,[1,3]]|delpaths([[-1,1]]) #=> [1,2,[1]]
						// jq-1.5: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2,[1,3]]
						// jq-master: [1,2,[1,3]]|delpaths([[-1]]) #=> [1,2]
						return oldval;
					} else {
						return jsonProvider.createMissing();
					}
				}, false);
			}

			output.emit(JsonNodeUtils.filter(jsonProvider, out, (val) -> jsonProvider.getNodeType(val) != JsonNodeType.MISSING), null);
		});
	}
}
