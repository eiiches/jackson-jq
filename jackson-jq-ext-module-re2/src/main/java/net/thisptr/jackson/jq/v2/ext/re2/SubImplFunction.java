package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class SubImplFunction implements Function {
	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Version version = bindContext.getJqVersion();
		Expression<Context, JsonNode> regexExpression = arguments.get(0);
		Expression<Context, JsonNode> replaceExpression = arguments.get(1);
		Expression<Context, JsonNode> flagsExpression = arguments.get(2);
		PrecompiledPatternPlan precompiled = PrecompiledPatternPlan.regexThenFlags(jsonProvider, regexExpression, flagsExpression, false);

		if (precompiled != null) {
			return FunctionBody.builder(arguments).usesInput(true).build((context, input, inputPath, output) -> {
				Preconditions.checkInputType(jsonProvider, "_sub_impl/3", input, JsonNodeType.STRING);
				for (Re2Pattern pattern : precompiled.patterns()) {
					List<JsonNode> match = match(jsonProvider, pattern, jsonProvider.getString(input));
					for (int i = 0; i < precompiled.flagsMultiplicity(); i++)
						replaceAndConcat(jsonProvider, context, output, match, replaceExpression, version);
				}
			});
		}

		return FunctionBody.builder(arguments).usesInput(true).build((context, input, inputPath, output) -> {
			Preconditions.checkInputType(jsonProvider, "_sub_impl/3", input, JsonNodeType.STRING);
			regexExpression.apply(context, input, UntrackedPath.getInstance(), (regex, regexPath) -> {
				Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 1, regex, JsonNodeType.STRING);
				flagsExpression.apply(context, input, UntrackedPath.getInstance(), (flags, flagsPath) -> {
					Preconditions.checkArgumentType(jsonProvider, "_sub_impl/3", 3, flags, JsonNodeType.STRING);
					Re2Pattern pattern = new Re2Pattern(jsonProvider.getString(regex), jsonProvider.getString(flags));
					List<JsonNode> match = match(jsonProvider, pattern, jsonProvider.getString(input));
					flagsExpression.apply(context, input, UntrackedPath.getInstance(), (ignored, ignoredPath) ->
							replaceAndConcat(jsonProvider, context, output, match, replaceExpression, version));
				});
			});
		});
	}

	private static <Context extends RuntimeContext, JsonNode> void replaceAndConcat(JsonProvider<JsonNode> jsonProvider, Context context, Output<JsonNode> output, List<JsonNode> match, Expression<Context, JsonNode> replaceExpression, Version version) throws JsonQueryException {
		Deque<Frame> frames = new ArrayDeque<>();
		frames.push(new Frame(match.size() - 1, null, null));

		while (!frames.isEmpty()) {
			Frame frame = frames.pop();
			if (frame.pendingException() != null)
				throw frame.pendingException();
			if (frame.index() < 0) {
				output.emit(jsonProvider.createString(concat(context.getRuntimeLimits(), frame.parts())), UntrackedPath.getInstance());
				continue;
			}

			JsonNode segment = match.get(frame.index());
			if (jsonProvider.isString(segment)) {
				frames.push(new Frame(frame.index() - 1, new Part(jsonProvider.getString(segment), frame.parts()), null));
				continue;
			}

			List<String> replacements = new ArrayList<>();
			@Var @Nullable JsonQueryException pendingException = null;
			try {
				replaceExpression.apply(context, segment, UntrackedPath.getInstance(), (replacement, outputPath) -> {
					JsonNodeType replacementType = jsonProvider.getNodeType(replacement);
					if (replacementType != JsonNodeType.STRING && replacementType != JsonNodeType.NULL)
						throw Preconditions.cannotBeAdded(jsonProvider, version, match.get(frame.index() - 1), replacement);
					replacements.add(replacementType == JsonNodeType.STRING ? jsonProvider.getString(replacement) : "");
				});
			} catch (JsonQueryException e) {
				pendingException = e;
			}
			if (pendingException != null)
				frames.push(new Frame(-1, null, pendingException));
			for (int i = replacements.size() - 1; i >= 0; i--)
				frames.push(new Frame(frame.index() - 1, new Part(replacements.get(i), frame.parts()), null));
		}
	}

	private static String concat(RuntimeLimits limits, @Nullable Part parts) {
		@Var long length = 0;
		for (@Nullable Part part = parts; part != null; part = part.next())
			length += part.value().length();
		RuntimeLimitChecks.checkStringLength(limits, length);

		StringBuilder result = new StringBuilder((int) length);
		for (@Nullable Part part = parts; part != null; part = part.next())
			result.append(part.value());
		return result.toString();
	}

	private record Frame(int index, @Nullable Part parts, @Nullable JsonQueryException pendingException) {
	}

	private record Part(String value, @Nullable Part next) {
	}

	private static <JsonNode> List<JsonNode> match(JsonProvider<JsonNode> jsonProvider, Re2Pattern pattern, String input) {
		List<JsonNode> result = new ArrayList<>();
		Matcher matcher = pattern.pattern.matcher(input);
		@Var int literalOffset = 0;
		@Var int searchOffset = 0;
		while (matcher.find(searchOffset)) {
			result.add(jsonProvider.createString(input.substring(literalOffset, matcher.start())));

			Map<String, JsonNode> captures = new LinkedHashMap<>();
			for (int group = 1; group <= matcher.groupCount(); group++) {
				String name = pattern.names[group];
				if (name == null)
					continue;
				String value = matcher.group(group);
				captures.put(name, value == null ? jsonProvider.createNull() : jsonProvider.createString(value));
			}
			result.add(jsonProvider.createObject(captures));

			literalOffset = matcher.end();
			if (!pattern.global)
				break;
			if (matcher.end() == input.length())
				break;
			searchOffset = matcher.start() == matcher.end()
					? matcher.end() + Character.charCount(input.codePointAt(matcher.end()))
					: matcher.end();
		}
		result.add(jsonProvider.createString(input.substring(literalOffset)));
		return result;
	}
}
