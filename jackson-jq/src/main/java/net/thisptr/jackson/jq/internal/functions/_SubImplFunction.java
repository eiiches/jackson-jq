package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
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
					replaceAndConcat(scope, output, match, args.get(1));
				});
			});
		});
	}

	private void replaceAndConcat(Scope scope, PathOutput output, List<JsonNode> match, Expression replaceExpr) throws JsonQueryException {
		final Deque<Frame> frames = new ArrayDeque<>();
		frames.push(new Frame(match.size() - 1, null, null));

		while (!frames.isEmpty()) {
			final Frame frame = frames.pop();
			if (frame.pendingException != null) {
				throw frame.pendingException;
			}
			if (frame.index < 0) {
				output.emit(new TextNode(concat(frame.parts)), null);
				continue;
			}

			final JsonNode segment = match.get(frame.index);
			if (segment.isTextual()) {
				frames.push(new Frame(frame.index - 1, new Part(segment.textValue(), frame.parts), null));
				continue;
			}

			final List<String> replacements = new ArrayList<>();
			JsonQueryException pendingException = null;
			try {
				replaceExpr.apply(scope, segment, replacement -> replacements.add(replacement.asText()));
			} catch (final JsonQueryException e) {
				pendingException = e;
			}
			if (pendingException != null) {
				frames.push(new Frame(-1, null, pendingException));
			}
			for (int i = replacements.size() - 1; i >= 0; --i) {
				frames.push(new Frame(frame.index - 1, new Part(replacements.get(i), frame.parts), null));
			}
		}
	}

	private static String concat(final Part parts) {
		final StringBuilder result = new StringBuilder();
		for (Part part = parts; part != null; part = part.next) {
			result.append(part.value);
		}
		return result.toString();
	}

	private static class Frame {
		private final int index;
		private final Part parts;
		private final JsonQueryException pendingException;

		private Frame(final int index, final Part parts, final JsonQueryException pendingException) {
			this.index = index;
			this.parts = parts;
			this.pendingException = pendingException;
		}
	}

	private static class Part {
		private final String value;
		private final Part next;

		private Part(final String value, final Part next) {
			this.value = value;
			this.next = next;
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
				for (int i = 1; i < regions.getNumRegs(); ++i) {
					final String name = pattern.names[i];
					if (name == null)
						continue;
					if (regions.getBeg(i) >= 0) {
						final String value = new String(inputBytes, regions.getBeg(i), regions.getEnd(i) - regions.getBeg(i), StandardCharsets.UTF_8);
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
