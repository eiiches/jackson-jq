package net.thisptr.jackson.jq.internal.functions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@BuiltinFunction("match/2")
public class MatchFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("match", in, JsonNodeType.STRING);

		final List<MatchObject> out = new ArrayList<>();
		final List<JsonNode> regexTuple = args.get(0).apply(scope, in);
		final List<JsonNode> modifiersTuple = args.get(1).apply(scope, in);
		for (final JsonNode regex : regexTuple)
			for (final JsonNode modifiers : modifiersTuple)
				out.addAll(match(in, regex, modifiers));

		final List<JsonNode> result = new ArrayList<>();
		for (final MatchObject m : out)
			result.add(scope.getObjectMapper().valueToTree(m));
		return result;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class CaptureObject {
		@JsonProperty("offset")
		public int offset;
		@JsonProperty("length")
		public int length;
		@JsonProperty("string")
		public String string;
		@JsonProperty("name")
		public String name;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	/* package private */static class MatchObject {
		@JsonProperty("offset")
		public int offset;
		@JsonProperty("length")
		public int length;
		@JsonProperty("string")
		public String string;
		@JsonProperty("captures")
		public List<CaptureObject> captures = new ArrayList<>();
	}

	public static List<MatchObject> match(final JsonNode haystack, final JsonNode regex, final JsonNode modifiers) throws JsonQueryException {
		if (!regex.isTextual())
			throw new JsonQueryException("regex argument to match() must be a string, got " + regex.getNodeType());
		if (!modifiers.isTextual() && !modifiers.isNull())
			throw new JsonQueryException("modifiers argument to match() must be a string, got " + modifiers.getNodeType());

		return match(haystack.asText(), regex.asText(), modifiers.isNull() ? "" : modifiers.asText());
	}

	private static Method namedGroupsMethod = null;
	static {
		try {
			namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
			namedGroupsMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {}
	}

	public static Map<Integer, String> namedGroups(final Pattern regex) {
		if (namedGroupsMethod == null)
			return null;

		try {
			@SuppressWarnings("unchecked")
			final Map<String, Integer> tmp = (Map<String, Integer>) namedGroupsMethod.invoke(regex);
			final Map<Integer, String> result = new HashMap<>();
			for (final Entry<String, Integer> e : tmp.entrySet())
				result.put(e.getValue(), e.getKey());
			return result;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return null;
		}
	}

	public static List<MatchObject> match(final String haystack, final String regex, final String modifiers) {
		final boolean flagGlobal = modifiers.contains("g");
		final boolean flagIgnoreEmpty = modifiers.contains("n");

		final List<MatchObject> result = new ArrayList<>();

		final Pattern pattern = Pattern.compile(regex, makeFlags(modifiers));
		final Map<Integer, String> namedGroups = namedGroups(pattern);

		final Matcher m = pattern.matcher(haystack);
		while (m.find()) {
			final MatchObject obj = new MatchObject();
			obj.offset = m.start();
			obj.length = m.end() - obj.offset;
			obj.string = m.group();

			if (flagIgnoreEmpty && obj.length == 0)
				continue;

			for (int i = 1; i <= m.groupCount(); ++i) {
				final CaptureObject capture = new CaptureObject();
				capture.offset = m.start(i);
				capture.length = m.end(i) - capture.offset;
				capture.string = m.group(i);
				if (namedGroups != null)
					capture.name = namedGroups.get(i);
				obj.captures.add(capture);
			}

			result.add(obj);

			if (!flagGlobal)
				break;
		}

		return result;
	}

	private static int makeFlags(final String modifiers) {
		int flags = 0;
		if (modifiers.contains("i"))
			flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
		if (modifiers.contains("m") || modifiers.contains("p"))
			flags |= Pattern.MULTILINE;
		if (modifiers.contains("s") || modifiers.contains("p"))
			flags |= Pattern.DOTALL;
		if (modifiers.contains("x"))
			flags |= Pattern.COMMENTS;
		return flags;
	}
}
