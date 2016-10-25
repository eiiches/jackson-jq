package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.OnigUtils;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;

@BuiltinFunction("_match_impl/3")
public class _MatchImplFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("_match_impl/3", in, JsonNodeType.STRING);
		final byte[] ibytes = in.asText().getBytes(StandardCharsets.UTF_8);
		final int[] cindex = UnicodeUtils.UTF8CharIndex(ibytes);

		final List<JsonNode> regexTuple = args.get(0).apply(scope, in);
		final List<JsonNode> modifiersTuple = args.get(1).apply(scope, in);
		final List<JsonNode> testTuple = args.get(2).apply(scope, in);

		final List<JsonNode> out = new ArrayList<>();

		for (final JsonNode regex : regexTuple) {
			Preconditions.checkArgumentType("_match_impl/3", 1, regex, JsonNodeType.STRING);

			for (final JsonNode modifiers : modifiersTuple) {
				Preconditions.checkArgumentType("_match_impl/3", 2, modifiers, JsonNodeType.STRING, JsonNodeType.NULL);

				for (final JsonNode test : testTuple) {
					Preconditions.checkArgumentType("_match_impl/3", 3, test, JsonNodeType.BOOLEAN);

					final OnigUtils.Pattern p = new OnigUtils.Pattern(regex.asText(), modifiers.isNull() ? null : modifiers.asText());
					out.add(match(scope.getObjectMapper(), p, ibytes, cindex, test.asBoolean()));
				}
			}
		}

		return out;
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

	private static JsonNode match(final ObjectMapper mapper, final OnigUtils.Pattern pattern, final byte[] ibytes, final int[] cindex, final boolean test) {
		final Matcher m = pattern.regex.matcher(ibytes);

		if (test) {
			final boolean match = m.search(0, ibytes.length, Option.NONE) >= 0;
			return BooleanNode.valueOf(match);
		} else {
			final ArrayNode matches = mapper.createArrayNode();

			int offset = 0;
			do {
				if (m.search(offset, ibytes.length, Option.NONE) < 0)
					break;

				final MatchObject obj = new MatchObject();
				obj.offset = cindex[m.getBegin()];
				obj.length = cindex[m.getEnd()] - cindex[m.getBegin()];
				obj.string = new String(ibytes, m.getBegin(), m.getEnd() - m.getBegin());

				// 1. regions is null when there is no capture groups
				// 2. for zero-width match, we do not include captures
				final Region regions = m.getRegion();
				if (regions != null && m.getEnd() != m.getBegin()) {
					for (int i = 1; i < regions.numRegs; ++i) {
						final CaptureObject capture = new CaptureObject();
						if (regions.beg[i] >= 0) {
							capture.offset = cindex[regions.beg[i]];
							capture.length = cindex[regions.end[i]] - cindex[regions.beg[i]];
							capture.string = new String(ibytes, regions.beg[i], regions.end[i] - regions.beg[i], StandardCharsets.UTF_8);
						} else {
							capture.offset = -1;
							capture.length = 0;
							capture.string = null;
						}
						capture.name = pattern.names[i];
						obj.captures.add(capture);
					}
				}

				matches.add(mapper.valueToTree(obj));

				if (m.getEnd() == offset) {
					++offset;
				} else {
					offset = m.getEnd();
				}
			} while (pattern.global && offset != ibytes.length);

			return matches;
		}
	}
}
