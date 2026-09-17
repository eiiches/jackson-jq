package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.errorprone.annotations.Var;
import com.google.re2j.Matcher;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

final class MatchImplFunction implements Function {
	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> regexExpression = arguments.get(0);
		Expression<Context, JsonNode> flagsExpression = arguments.get(1);
		Expression<Context, JsonNode> testExpression = arguments.get(2);
		PrecompiledPatternPlan precompiled = PrecompiledPatternPlan.flagsThenRegex(jsonProvider, regexExpression, flagsExpression, true);

		if (precompiled != null) {
			return FunctionBody.builder(arguments).usesInput(true).build((context, input, inputPath, output) -> {
				Preconditions.checkInputType(jsonProvider, "_match_impl/3", input, JsonNodeType.STRING);
				String inputText = jsonProvider.getString(input);
				testExpression.apply(context, input, UntrackedPath.getInstance(), (test, outputPath) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
					for (Re2Pattern pattern : precompiled.patterns())
						output.emit(match(jsonProvider, pattern, inputText, jsonProvider.getBoolean(test)), UntrackedPath.getInstance());
				});
			});
		}

		return FunctionBody.builder(arguments).usesInput(true).build((context, input, inputPath, output) -> {
			Preconditions.checkInputType(jsonProvider, "_match_impl/3", input, JsonNodeType.STRING);
			String inputText = jsonProvider.getString(input);
			testExpression.apply(context, input, UntrackedPath.getInstance(), (test, outputPath) -> {
				Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
				flagsExpression.apply(context, input, UntrackedPath.getInstance(), (flags, flagsPath) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
					regexExpression.apply(context, input, UntrackedPath.getInstance(), (regex, regexPath) -> {
						Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 1, regex, JsonNodeType.STRING);
						String flagsText = jsonProvider.isNull(flags) ? null : jsonProvider.getString(flags);
						Re2Pattern pattern = new Re2Pattern(jsonProvider.getString(regex), flagsText);
						output.emit(match(jsonProvider, pattern, inputText, jsonProvider.getBoolean(test)), UntrackedPath.getInstance());
					});
				});
			});
		});
	}

	private static final class CaptureObject {
		private int offset;
		private int length;
		private @Nullable String string;
		private @Nullable String name;
	}

	private static final class MatchObject {
		private int offset;
		private int length;
		private @Nullable String string;
		private final List<CaptureObject> captures = new ArrayList<>();
	}

	private static <JsonNode> JsonNode captureToJson(JsonProvider<JsonNode> jsonProvider, CaptureObject capture) {
		JsonNode length = jsonProvider.createNumber(capture.length);
		JsonNode string = capture.string == null ? jsonProvider.createNull() : jsonProvider.createString(capture.string);

		Map<String, JsonNode> node = new LinkedHashMap<>();
		node.put("offset", jsonProvider.createNumber(capture.offset));
		if (capture.length == 0) {
			node.put("string", string);
			node.put("length", length);
		} else {
			node.put("length", length);
			node.put("string", string);
		}
		node.put("name", capture.name == null ? jsonProvider.createNull() : jsonProvider.createString(capture.name));
		return jsonProvider.createObject(node);
	}

	private static <JsonNode> JsonNode matchToJson(JsonProvider<JsonNode> jsonProvider, MatchObject match) {
		List<JsonNode> captures = new ArrayList<>(match.captures.size());
		for (CaptureObject capture : match.captures)
			captures.add(captureToJson(jsonProvider, capture));
		Map<String, JsonNode> node = new LinkedHashMap<>();
		node.put("offset", jsonProvider.createNumber(match.offset));
		node.put("length", jsonProvider.createNumber(match.length));
		node.put("string", match.string == null ? jsonProvider.createNull() : jsonProvider.createString(match.string));
		node.put("captures", jsonProvider.createArray(captures));
		return jsonProvider.createObject(node);
	}

	private static <JsonNode> JsonNode match(JsonProvider<JsonNode> jsonProvider, Re2Pattern pattern, String input, boolean test) {
		Matcher matcher = pattern.pattern.matcher(input);
		if (test)
			return jsonProvider.createBoolean(matcher.find());

		UnicodeIndex index = new UnicodeIndex(input);
		List<JsonNode> matches = new ArrayList<>();
		@Var int searchOffset = 0;
		while (matcher.find(searchOffset)) {
			MatchObject match = new MatchObject();
			match.offset = index.at(matcher.start());
			match.length = index.at(matcher.end()) - match.offset;
			match.string = matcher.group();

			if (matcher.start() != matcher.end()) {
				for (int group = 1; group <= matcher.groupCount(); group++) {
					CaptureObject capture = new CaptureObject();
					if (matcher.start(group) >= 0) {
						capture.offset = index.at(matcher.start(group));
						capture.length = index.at(matcher.end(group)) - capture.offset;
						capture.string = matcher.group(group);
					} else {
						capture.offset = -1;
						capture.length = 0;
						capture.string = null;
					}
					capture.name = pattern.names[group];
					match.captures.add(capture);
				}
			}

			matches.add(matchToJson(jsonProvider, match));
			if (!pattern.global)
				break;
			if (matcher.end() == input.length())
				break;
			searchOffset = matcher.start() == matcher.end()
					? matcher.end() + Character.charCount(input.codePointAt(matcher.end()))
					: matcher.end();
		}
		return jsonProvider.createArray(matches);
	}
}
