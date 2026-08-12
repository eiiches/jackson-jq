package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration("contains/1")
public class ContainsFunction<JsonNode> implements Function {

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <N> void apply(final Scope<N> scope, final List<Expression<N>> args, final N in, final Path<N> ipath, final PathOutput<N> output, final Version version) throws JsonQueryException {
		applyInternal((Scope) scope, (List) args, (JsonNode) in, (Path) ipath, (PathOutput) output, version);
	}

	private void applyInternal(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		args.get(0).apply(scope, in, (value) -> {
			if (jsonProvider.getNodeType(in) != jsonProvider.getNodeType(value)
					|| (jsonProvider.getNodeType(in) == JsonNodeType.BOOLEAN && jsonProvider.asBoolean(in) != jsonProvider.asBoolean(value))) {
				throw new JsonQueryTypeException(jsonProvider, "%s and %s cannot have their containment checked", in, value);
			}
			output.emit(jsonProvider.createBoolean(contains(jsonProvider, value, in)), null);
		});
	}

	private boolean contains(final JsonProvider<JsonNode> jsonProvider, final JsonNode needle, final JsonNode haystack) {
		final JsonNodeType hType = jsonProvider.getNodeType(haystack);
		final JsonNodeType nType = jsonProvider.getNodeType(needle);
		if (hType == JsonNodeType.STRING && nType == JsonNodeType.STRING) {
			return jsonProvider.asText(haystack).contains(jsonProvider.asText(needle));
		} else if (hType == JsonNodeType.ARRAY && nType == JsonNodeType.ARRAY) {
			final Iterator<JsonNode> nIter = jsonProvider.elements(needle);
			while (nIter.hasNext()) {
				final JsonNode n = nIter.next();
				boolean found = false;
				final Iterator<JsonNode> hIter = jsonProvider.elements(haystack);
				while (hIter.hasNext()) {
					final JsonNode h = hIter.next();
					if (contains(jsonProvider, n, h)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		} else if (hType == JsonNodeType.OBJECT && nType == JsonNodeType.OBJECT) {
			final Iterator<Entry<String, JsonNode>> iter = jsonProvider.fields(needle);
			while (iter.hasNext()) {
				final Entry<String, JsonNode> field = iter.next();
				final JsonNode tmp = jsonProvider.get(haystack, field.getKey());
				if (tmp == null)
					return false;
				if (!contains(jsonProvider, field.getValue(), tmp))
					return false;
			}
			return true;
		} else {
			return new JsonNodeComparator<>(jsonProvider).compare(haystack, needle) == 0;
		}
	}
}
