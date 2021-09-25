package net.thisptr.jackson.jq.internal.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("contains/1")
public class ContainsFunction implements Function {
	private static final JsonNodeComparator COMPARATOR = JsonNodeComparator.getInstance();

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		args.get(0).apply(scope, in, (value) -> {
			if (in.getNodeType() != value.getNodeType()
					|| (in.isBoolean() && in.asBoolean() != value.asBoolean())) {
				throw new JsonQueryTypeException("%s and %s cannot have their containment checked", in, value);
			}
			output.emit(BooleanNode.valueOf(contains(value, in)), null);
		});
	}

	private static boolean contains(final JsonNode needle, final JsonNode haystack) {
		if (haystack.isTextual() && needle.isTextual()) {
			return haystack.asText().contains(needle.asText());
		} else if (haystack.isArray() && needle.isArray()) {
			for (final JsonNode n : needle) {
				boolean found = false;
				for (final JsonNode h : haystack) {
					if (contains(n, h)) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
			return true;
		} else if (haystack.isObject() && needle.isObject()) {
			final Iterator<Entry<String, JsonNode>> iter = needle.fields();
			while (iter.hasNext()) {
				final Entry<String, JsonNode> field = iter.next();
				final JsonNode tmp = haystack.get(field.getKey());
				if (tmp == null)
					return false;
				if (!contains(field.getValue(), tmp))
					return false;
			}
			return true;
		} else {
			return COMPARATOR.compare(haystack, needle) == 0;
		}
	}
}
