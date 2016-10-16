package net.thisptr.jackson.jq.internal.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("_sub_impl/3")
public class _SubImplFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("_sub_impl/3", in, JsonNodeType.STRING);

		final List<JsonNode> result = new ArrayList<>();

		for (final JsonNode regexText : args.get(0).apply(scope, in)) {
			Preconditions.checkArgumentType("_sub_impl/3", 1, regexText, JsonNodeType.STRING);

			for (final JsonNode flagsText : args.get(2).apply(scope, in)) {
				Preconditions.checkArgumentType("_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

				final OnigUtils.Pattern p = new OnigUtils.Pattern(regexText.asText(), flagsText.asText());
				concat(result, sub(scope, p, in.asText(), args.get(1)));
			}
		}

		return result;
	}

	private static void concat(final List<JsonNode> out, final int index, final Stack<JsonNode> stack, final List<List<JsonNode>> values) {
		if (index == values.size()) {
			final StringBuilder builder = new StringBuilder();
			for (final JsonNode item : stack) {
				if (item.isTextual()) {
					builder.append(item.asText());
				} else {
					builder.append(item.toString());
				}
			}
			out.add(TextNode.valueOf(builder.toString()));
		} else {
			for (final JsonNode value : values.get(index)) {
				stack.push(value);
				concat(out, index + 1, stack, values);
				stack.pop();
			}
		}
	}

	private static void concat(final List<JsonNode> out, final List<List<JsonNode>> values) {
		concat(out, 0, new Stack<>(), values);
	}

	private static List<List<JsonNode>> sub(final Scope scope, final OnigUtils.Pattern pattern, final String inputText, final JsonQuery replace) throws JsonQueryException {
		final List<List<JsonNode>> result = new ArrayList<>();

		final byte[] inputBytes = inputText.getBytes();
		final Matcher m = pattern.regex.matcher(inputBytes);
		int offset = 0;
		do {
			if (m.search(offset, inputBytes.length, Option.NONE) < 0)
				break;

			result.add(Collections.singletonList(TextNode.valueOf(new String(inputBytes, offset, m.getBegin() - offset))));

			final ObjectNode captures = scope.getObjectMapper().createObjectNode();
			final Region regions = m.getRegion();
			if (regions != null) {
				for (int i = 1; i < regions.numRegs; ++i) {
					final String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.beg[i] >= 0) {
						final String value = new String(inputBytes, regions.beg[i], regions.end[i] - regions.beg[i]);
						captures.set(name, TextNode.valueOf(value));
					} else {
						captures.set(name, NullNode.getInstance());
					}
				}
			}

			result.add(replace.apply(scope, captures));

			offset = m.getEnd();
		} while (pattern.global && offset != inputBytes.length);

		result.add(Collections.singletonList(TextNode.valueOf(new String(inputBytes, offset, inputBytes.length - offset))));
		return result;
	}
}