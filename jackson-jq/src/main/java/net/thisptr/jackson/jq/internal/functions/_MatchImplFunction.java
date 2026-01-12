package net.thisptr.jackson.jq.internal.functions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import net.thisptr.jackson.jq.internal.misc.UnicodeUtils;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("_match_impl/3")
public class _MatchImplFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "_match_impl/3", in, JsonNodeType.STRING);
		final byte[] ibytes = jsonProvider.asText(in).getBytes(StandardCharsets.UTF_8);
		final int[] cindex = UnicodeUtils.UTF8CharIndex(ibytes);

		args.get(2).apply(scope, in, (test) -> {
			Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
			args.get(1).apply(scope, in, (flags) -> {
				Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
				args.get(0).apply(scope, in, (regex) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 1, regex, JsonNodeType.STRING);
					final OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.asText(regex), jsonProvider.getNodeType(flags) == JsonNodeType.NULL ? null : jsonProvider.asText(flags));
					output.emit(match(jsonProvider, p, ibytes, cindex, jsonProvider.asBoolean(test)), null);
				});
			});
		});
	}

	private static class CaptureObject {
		public int offset;
		public int length;
		public String string;
		public String name;
	}

	/* package private */static class MatchObject {
		public int offset;
		public int length;
		public String string;
		public List<CaptureObject> captures = new ArrayList<>();
	}

	private static <JsonNode> JsonNode captureToJson(final JsonProvider<JsonNode> jsonProvider, final CaptureObject capture) {
		JsonNode node = jsonProvider.createObject();
		node = jsonProvider.set(node, "offset", jsonProvider.createInt(capture.offset));
		node = jsonProvider.set(node, "length", jsonProvider.createInt(capture.length));
		node = jsonProvider.set(node, "string", capture.string == null ? jsonProvider.createNull() : jsonProvider.createString(capture.string));
		node = jsonProvider.set(node, "name", capture.name == null ? jsonProvider.createNull() : jsonProvider.createString(capture.name));
		return node;
	}

	private static <JsonNode> JsonNode matchToJson(final JsonProvider<JsonNode> jsonProvider, final MatchObject obj) {
		JsonNode node = jsonProvider.createObject();
		node = jsonProvider.set(node, "offset", jsonProvider.createInt(obj.offset));
		node = jsonProvider.set(node, "length", jsonProvider.createInt(obj.length));
		node = jsonProvider.set(node, "string", jsonProvider.createString(obj.string));
		JsonNode capturesArray = jsonProvider.createArray();
		for (final CaptureObject capture : obj.captures) {
			capturesArray = jsonProvider.add(capturesArray, captureToJson(jsonProvider, capture));
		}
		node = jsonProvider.set(node, "captures", capturesArray);
		return node;
	}

	private static <JsonNode> JsonNode match(final JsonProvider<JsonNode> jsonProvider, final OnigUtils.Pattern pattern, final byte[] ibytes, final int[] cindex, final boolean test) {
		final Matcher m = pattern.regex.matcher(ibytes);

		if (test) {
			final boolean match = m.search(0, ibytes.length, Option.NONE) >= 0;
			return jsonProvider.createBoolean(match);
		} else {
			JsonNode matches = jsonProvider.createArray();

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
					for (int i = 1; i < regions.getNumRegs(); ++i) {
						final CaptureObject capture = new CaptureObject();
						if (regions.getBeg(i) >= 0) {
							capture.offset = cindex[regions.getBeg(i)];
							capture.length = cindex[regions.getEnd(i)] - cindex[regions.getBeg(i)];
							capture.string = new String(ibytes, regions.getBeg(i), regions.getEnd(i) - regions.getBeg(i), StandardCharsets.UTF_8);
						} else {
							capture.offset = -1;
							capture.length = 0;
							capture.string = null;
						}
						capture.name = pattern.names[i];
						obj.captures.add(capture);
					}
				}

				matches = jsonProvider.add(matches, matchToJson(jsonProvider, obj));

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
