package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("_sub_impl/3")
public class _SubImplFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "_sub_impl/3", in, JsonNodeType.STRING);

		args.get(0).apply(scope, in, (regexText) -> {
			Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 1, regexText, JsonNodeType.STRING);

			args.get(2).apply(scope, in, (flagsText) -> {
				Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

				final OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.asText(regexText), jsonProvider.asText(flagsText));
				final List<JsonNode> match = match(jsonProvider, p, jsonProvider.asText(in));

				// This just repeats same emit()s the number of times as the number of flags. This is to emulate jq behavior (which is probably a bug).
				args.get(2).apply(scope, in, (dummy) -> {
					replaceAndConcat(scope, jsonProvider, new Stack<>(), output, match, args.get(1), in, args.get(2));
				});
			});
		});
	}

	private void replaceAndConcat(Scope<JsonNode> scope, JsonProvider<JsonNode> jsonProvider, Stack<String> stack, PathOutput<JsonNode> output, List<JsonNode> match, Expression<JsonNode> replaceExpr, final JsonNode in, final Expression<JsonNode> flags) throws JsonQueryException {
		if (match.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (int i = stack.size() - 1; i >= 0; --i) {
				sb.append(stack.get(i));
			}
			output.emit(jsonProvider.createString(sb.toString()), null);
			return;
		}

		final JsonNode rhead = match.get(match.size() - 1);
		final List<JsonNode> rtail = match.subList(0, match.size() - 1);

		if (jsonProvider.getNodeType(rhead) == JsonNodeType.STRING) {
			stack.push(jsonProvider.asText(rhead));
			replaceAndConcat(scope, jsonProvider, stack, output, rtail, replaceExpr, in, flags);
			stack.pop();
		} else {
			replaceExpr.apply(scope, rhead, (replacement) -> {
				stack.push(jsonProvider.asText(replacement));
				replaceAndConcat(scope, jsonProvider, stack, output, rtail, replaceExpr, in, flags);
				stack.pop();
			});
		}
	}

	private static <JsonNode> List<JsonNode> match(final JsonProvider<JsonNode> jsonProvider, final OnigUtils.Pattern pattern, final String inputText) {
		final List<JsonNode> result = new ArrayList<>();

		final byte[] inputBytes = inputText.getBytes(StandardCharsets.UTF_8);
		final Matcher m = pattern.regex.matcher(inputBytes);
		int offset = 0;
		do {
			if (m.search(offset, inputBytes.length, Option.NONE) < 0)
				break;

			result.add(jsonProvider.createString(new String(inputBytes, offset, m.getBegin() - offset, StandardCharsets.UTF_8)));

			JsonNode captures = jsonProvider.createObject();
			final Region regions = m.getRegion();
			if (regions != null) {
				for (int i = 1; i < regions.getNumRegs(); ++i) {
					final String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.getBeg(i) >= 0) {
						final String value = new String(inputBytes, regions.getBeg(i), regions.getEnd(i) - regions.getBeg(i), StandardCharsets.UTF_8);
						captures = jsonProvider.set(captures, name, jsonProvider.createString(value));
					} else {
						captures = jsonProvider.set(captures, name, jsonProvider.createNull());
					}
				}
			}

			result.add(captures);

			offset = m.getEnd();
		} while (pattern.global && offset != inputBytes.length);

		result.add(jsonProvider.createString(new String(inputBytes, offset, inputBytes.length - offset, StandardCharsets.UTF_8)));
		return result;
	}
}
