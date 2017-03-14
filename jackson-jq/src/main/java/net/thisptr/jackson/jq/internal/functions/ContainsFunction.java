package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.JsonNodeComparator;

@BuiltinFunction("contains/1")
public class ContainsFunction implements Function {
	private static final JsonNodeComparator COMPARATOR = JsonNodeComparator.getInstance();

	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (final JsonNode value : args.get(0).apply(scope, in)) {
			if (in.getNodeType() != value.getNodeType()
					|| (in.isBoolean() && in.asBoolean() != value.asBoolean())) {
				throw JsonQueryTypeException.format("%s (%s) and %s (%s) cannot have their containment checked", in.getNodeType(), in, value.getNodeType(), value);
			}
			out.add(BooleanNode.valueOf(contains(value, in)));
		}
		return out;
	}

	private static boolean contains(final JsonNode needle, final JsonNode haystack) throws JsonQueryException {
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
				if (found == false)
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
