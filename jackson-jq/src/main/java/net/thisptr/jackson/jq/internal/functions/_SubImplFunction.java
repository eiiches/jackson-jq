package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
		List<StringBuilder> results = new ArrayList<>();
		results.add(new StringBuilder());

		for (final JsonNode segment : match) {
			if (segment.isTextual()) {
				final String text = segment.textValue();
				for (final StringBuilder result : results) {
					result.append(text);
				}
			} else {
				final List<String> replacements = new ArrayList<>();
				replaceExpr.apply(scope, segment, replacement -> replacements.add(replacement.asText()));

				final List<StringBuilder> next = new ArrayList<>(results.size() * replacements.size());
				for (final StringBuilder result : results) {
					for (final String replacement : replacements) {
						next.add(new StringBuilder(result).append(replacement));
					}
				}
				results = next;
			}
		}

		for (final StringBuilder result : results) {
			output.emit(new TextNode(result.toString()), null);
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
