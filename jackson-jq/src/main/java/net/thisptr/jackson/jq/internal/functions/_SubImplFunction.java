package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
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

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

@BuiltinFunction("_sub_impl/3")
public class _SubImplFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		Preconditions.checkInputType("_sub_impl/3", in, JsonNodeType.STRING);

		args.get(0).apply(scope, in, (regexText) -> {
			Preconditions.checkArgumentType("_sub_impl/3", 1, regexText, JsonNodeType.STRING);

			args.get(2).apply(scope, in, (flagsText) -> {
				Preconditions.checkArgumentType("_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

				final OnigUtils.Pattern p = new OnigUtils.Pattern(regexText.asText(), flagsText.asText());
				concat(output, sub(scope, p, in.asText(), args.get(1)));
			});
		});
	}

	private static void concat(final Output output, final int index, final Stack<JsonNode> stack, final List<List<JsonNode>> values) throws JsonQueryException {
		if (index == values.size()) {
			final StringBuilder builder = new StringBuilder();
			for (final JsonNode item : stack) {
				if (item.isTextual()) {
					builder.append(item.asText());
				} else {
					builder.append(item.toString());
				}
			}
			output.emit(TextNode.valueOf(builder.toString()));
		} else {
			for (final JsonNode value : values.get(index)) {
				stack.push(value);
				concat(output, index + 1, stack, values);
				stack.pop();
			}
		}
	}

	private static void concat(final Output output, final List<List<JsonNode>> values) throws JsonQueryException {
		concat(output, 0, new Stack<>(), values);
	}

	private static List<List<JsonNode>> sub(final Scope scope, final OnigUtils.Pattern pattern, final String inputText, final Expression replace) throws JsonQueryException {
		final List<List<JsonNode>> result = new ArrayList<>();

		final byte[] inputBytes = inputText.getBytes(StandardCharsets.UTF_8);
		final Matcher m = pattern.regex.matcher(inputBytes);
		int offset = 0;
		do {
			if (m.search(offset, inputBytes.length, Option.NONE) < 0)
				break;

			result.add(Collections.singletonList(TextNode.valueOf(new String(inputBytes, offset, m.getBegin() - offset, StandardCharsets.UTF_8))));

			final ObjectNode captures = scope.getObjectMapper().createObjectNode();
			final Region regions = m.getRegion();
			if (regions != null) {
				for (int i = 1; i < regions.numRegs; ++i) {
					final String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.beg[i] >= 0) {
						final String value = new String(inputBytes, regions.beg[i], regions.end[i] - regions.beg[i], StandardCharsets.UTF_8);
						captures.set(name, TextNode.valueOf(value));
					} else {
						captures.set(name, NullNode.getInstance());
					}
				}
			}

			final List<JsonNode> tmp = new ArrayList<>();
			replace.apply(scope, captures, tmp::add);

			result.add(tmp);

			offset = m.getEnd();
		} while (pattern.global && offset != inputBytes.length);

		result.add(Collections.singletonList(TextNode.valueOf(new String(inputBytes, offset, inputBytes.length - offset, StandardCharsets.UTF_8))));
		return result;
	}
}