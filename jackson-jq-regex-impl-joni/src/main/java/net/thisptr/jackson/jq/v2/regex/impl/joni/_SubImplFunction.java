package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

@AutoService(FunctionFactory.class)
@FunctionRegistration(name = "_sub_impl", nargs = 3)
public class _SubImplFunction implements FunctionFactory {
	@Override
	public <JsonNode> Function<JsonNode> createFunction(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		Expression<JsonNode> regexExpr = args.get(0);
		Expression<JsonNode> replaceExpr = args.get(1);
		Expression<JsonNode> flagsExpr = args.get(2);

		return (frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "_sub_impl/3", in, JsonNodeType.STRING);

			regexExpr.apply(frame, in, (regexText) -> {
				Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 1, regexText, JsonNodeType.STRING);

				flagsExpr.apply(frame, in, (flagsText) -> {
					Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

					OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.asText(regexText), jsonProvider.asText(flagsText));
					List<JsonNode> match = match(jsonProvider, p, jsonProvider.asText(in));

					// This just repeats same emit()s the number of times as the number of flags. This is to emulate jq behavior (which is probably a bug).
					flagsExpr.apply(frame, in, (dummy) -> {
						replaceAndConcat(jsonProvider, frame, new Stack<>(), output, match, replaceExpr, in, flagsExpr);
					});
				});
			});
		};
	}

	private <JsonNode> void replaceAndConcat(JsonProvider<JsonNode> jsonProvider, @Nullable StackFrame<JsonNode> frame, Stack<String> stack, PathOutput<JsonNode> output, List<JsonNode> match, Expression<JsonNode> replaceExpr, JsonNode in, Expression<JsonNode> flags) throws JsonQueryException {
		if (match.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (int i = stack.size() - 1; i >= 0; --i) {
				sb.append(stack.get(i));
			}
			output.emit(jsonProvider.createString(sb.toString()), null);
			return;
		}

		JsonNode rhead = match.get(match.size() - 1);
		List<JsonNode> rtail = match.subList(0, match.size() - 1);

		if (jsonProvider.getNodeType(rhead) == JsonNodeType.STRING) {
			stack.push(jsonProvider.asText(rhead));
			replaceAndConcat(jsonProvider, frame, stack, output, rtail, replaceExpr, in, flags);
			stack.pop();
		} else {
			replaceExpr.apply(frame, rhead, (replacement) -> {
				stack.push(jsonProvider.asText(replacement));
				replaceAndConcat(jsonProvider, frame, stack, output, rtail, replaceExpr, in, flags);
				stack.pop();
			});
		}
	}

	private static <JsonNode> List<JsonNode> match(JsonProvider<JsonNode> jsonProvider, OnigUtils.Pattern pattern, String inputText) {
		List<JsonNode> result = new ArrayList<>();

		byte[] inputBytes = inputText.getBytes(StandardCharsets.UTF_8);
		Matcher m = pattern.regex.matcher(inputBytes);
		@Var int offset = 0;
		do {
			if (m.search(offset, inputBytes.length, Option.NONE) < 0)
				break;

			result.add(jsonProvider.createString(new String(inputBytes, offset, m.getBegin() - offset, StandardCharsets.UTF_8)));

			@Var JsonNode captures = jsonProvider.createObject();
			Region regions = m.getRegion();
			if (regions != null) {
				for (int i = 1; i < regions.getNumRegs(); ++i) {
					String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.getBeg(i) >= 0) {
						String value = new String(inputBytes, regions.getBeg(i), regions.getEnd(i) - regions.getBeg(i), StandardCharsets.UTF_8);
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
