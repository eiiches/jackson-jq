package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.regex.impl.joni.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

@AutoService(Function.class)
@FunctionRegistration(name = "_sub_impl", nargs = 3)
public class _SubImplFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		Expression<Context, JsonNode> regexExpr = args.get(0);
		Expression<Context, JsonNode> replaceExpr = args.get(1);
		Expression<Context, JsonNode> flagsExpr = args.get(2);
		PrecompiledPatternPlan precompiled = PrecompiledPatternPlan.regexThenFlags(jsonProvider, regexExpr, flagsExpr, false);

		if (precompiled != null) {
			return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
				Preconditions.checkInputType(jsonProvider, "_sub_impl/3", in, JsonNodeType.STRING);
				for (OnigUtils.Pattern pattern : precompiled.patterns()) {
					List<JsonNode> match = match(jsonProvider, pattern, jsonProvider.getString(in));
					for (int i = 0; i < precompiled.flagsMultiplicity(); i++)
						replaceAndConcat(jsonProvider, frame, new ArrayDeque<>(), output, match, replaceExpr, in, flagsExpr, version);
				}
			});
		}

		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "_sub_impl/3", in, JsonNodeType.STRING);

			regexExpr.apply(frame, in, UntrackedPath.getInstance(), (regexText, opath) -> {
				Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 1, regexText, JsonNodeType.STRING);

				flagsExpr.apply(frame, in, UntrackedPath.getInstance(), (flagsText, opath2) -> {
					Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 3, flagsText, JsonNodeType.STRING);

					OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.getString(regexText), jsonProvider.getString(flagsText));
					List<JsonNode> match = match(jsonProvider, p, jsonProvider.getString(in));

					// This just repeats same emit()s the number of times as the number of flags. This is to emulate jq behavior (which is probably a bug).
					flagsExpr.apply(frame, in, UntrackedPath.getInstance(), (dummy, opath3) -> {
						replaceAndConcat(jsonProvider, frame, new ArrayDeque<>(), output, match, replaceExpr, in, flagsExpr, version);
					});
				});
			});
		});
	}

	private <Context, JsonNode> void replaceAndConcat(JsonProvider<JsonNode> jsonProvider, Context context, Deque<String> stack, Output<JsonNode> output, List<JsonNode> match, Expression<Context, JsonNode> replaceExpr, JsonNode in, Expression<Context, JsonNode> flags, Version version) throws JsonQueryException {
		if (match.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String s : stack) {
				sb.append(s);
			}
			output.emit(jsonProvider.createString(sb.toString()), UntrackedPath.getInstance());
			return;
		}

		JsonNode rhead = match.get(match.size() - 1);
		List<JsonNode> rtail = match.subList(0, match.size() - 1);

		if (jsonProvider.isString(rhead)) {
			stack.push(jsonProvider.getString(rhead));
			replaceAndConcat(jsonProvider, context, stack, output, rtail, replaceExpr, in, flags, version);
			stack.pop();
		} else {
			replaceExpr.apply(context, rhead, UntrackedPath.getInstance(), (replacement, opath) -> {
				// jq concatenates the replacement onto the text preceding the match, so a null
				// replacement contributes nothing and anything else non-string is a type error.
				JsonNodeType replacementType = jsonProvider.getNodeType(replacement);
				if (replacementType != JsonNodeType.STRING && replacementType != JsonNodeType.NULL) {
					// jq names the preceding literal as the left operand. match() alternates
					// [literal, captures, ..., literal], so a captures rhead always leaves one at
					// the end of rtail. We replace right-to-left where jq goes left-to-right, so
					// with several matches this names the last one's literal, not the first one's.
					throw Preconditions.cannotBeAdded(jsonProvider, version, rtail.get(rtail.size() - 1), replacement);
				}
				stack.push(replacementType == JsonNodeType.STRING ? jsonProvider.getString(replacement) : "");
				replaceAndConcat(jsonProvider, context, stack, output, rtail, replaceExpr, in, flags, version);
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

			Map<String, JsonNode> captures = new LinkedHashMap<>();
			Region regions = m.getRegion();
			if (regions != null) {
				for (int i = 1; i < regions.getNumRegs(); ++i) {
					String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.getBeg(i) >= 0) {
						String value = new String(inputBytes, regions.getBeg(i), regions.getEnd(i) - regions.getBeg(i), StandardCharsets.UTF_8);
						captures.put(name, jsonProvider.createString(value));
					} else {
						captures.put(name, jsonProvider.createNull());
					}
				}
			}

			result.add(jsonProvider.createObject(captures));

			offset = m.getEnd();
		} while (pattern.global && offset != inputBytes.length);

		result.add(jsonProvider.createString(new String(inputBytes, offset, inputBytes.length - offset, StandardCharsets.UTF_8)));
		return result;
	}
}
