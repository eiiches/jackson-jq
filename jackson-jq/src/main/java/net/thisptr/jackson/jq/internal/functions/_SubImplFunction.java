package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@BuiltinFunction("_sub_impl/3")
public class _SubImplFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("_sub_impl/3", in, JsonNodeType.STRING);

		args.get(0).apply(scope, in, (regexText) -> {
			Preconditions.checkArgumentType("_sub_impl/3", 1, regexText, JsonNodeType.STRING);

			args.get(2).apply(scope, in, (flagsText) -> {
				Preconditions.checkArgumentType("_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

				final OnigUtils.Pattern p = new OnigUtils.Pattern(regexText.asText(), flagsText.asText());
				final List<JsonNode> match = match(scope.getObjectMapper(), p, in.asText());

				// This just repeats same emit()s the number of times as the number of flags. This is to emulate jq behavior (which is probably a bug).
				args.get(2).apply(scope, in, (dummy) -> {
					replaceAndConcat(scope, new Stack<>(), output, match, args.get(1), in, args.get(2));
				});
			});
		});
	}

	private void replaceAndConcat(Scope scope, Stack<String> stack, PathOutput output, List<JsonNode> match, Expression replaceExpr, final JsonNode in, final Expression flags) throws JsonQueryException {
		if (match.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (int i = stack.size() - 1; i >= 0; --i) {
				sb.append(stack.get(i));
			}
			output.emit(new TextNode(sb.toString()), null);
			return;
		}

		final JsonNode rhead = match.get(match.size() - 1);
		final List<JsonNode> rtail = match.subList(0, match.size() - 1);

		if (rhead.isTextual()) {
			stack.push(rhead.textValue());
			replaceAndConcat(scope, stack, output, rtail, replaceExpr, in, flags);
			stack.pop();
		} else {
			replaceExpr.apply(scope, rhead, (replacement) -> {
				stack.push(replacement.asText());
				replaceAndConcat(scope, stack, output, rtail, replaceExpr, in, flags);
				stack.pop();
			});
		}
	}

	private static List<JsonNode> match(final ObjectMapper mapper, final OnigUtils.Pattern pattern, final String inputText) {
		final List<JsonNode> result = new ArrayList<>();

		final byte[] inputBytes = inputText.getBytes(StandardCharsets.UTF_8);
		final Matcher m = pattern.regex.matcher(inputBytes);
		int offset = 0;
		do {
			if (m.search(offset, inputBytes.length, Option.NONE) < 0)
				break;

			result.add(TextNode.valueOf(new String(inputBytes, offset, m.getBegin() - offset, StandardCharsets.UTF_8)));

			final ObjectNode captures = mapper.createObjectNode();
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

			result.add(captures);

			offset = m.getEnd();
		} while (pattern.global && offset != inputBytes.length);

		result.add(TextNode.valueOf(new String(inputBytes, offset, inputBytes.length - offset, StandardCharsets.UTF_8)));
		return result;
	}
}