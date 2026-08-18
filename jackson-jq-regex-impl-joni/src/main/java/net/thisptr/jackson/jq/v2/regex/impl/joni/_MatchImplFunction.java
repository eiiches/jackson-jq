package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@AutoService(Function.class)
@FunctionRegistration(name = "_match_impl", nargs = 3)
public class _MatchImplFunction implements Function {
	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		Expression<JsonNode> regexExpr = args.get(0);
		Expression<JsonNode> flagsExpr = args.get(1);
		Expression<JsonNode> testExpr = args.get(2);

		return (frame, in, ipath, output, ignoredRequirePath) -> {
			Preconditions.checkInputType(jsonProvider, "_match_impl/3", in, JsonNodeType.STRING);
			byte[] ibytes = jsonProvider.asText(in).getBytes(StandardCharsets.UTF_8);
			int[] cindex = UnicodeUtils.utf8CharIndex(ibytes);

			testExpr.apply(frame, in, (test) -> {
				Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
				flagsExpr.apply(frame, in, (flags) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
					regexExpr.apply(frame, in, (regex) -> {
						Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 1, regex, JsonNodeType.STRING);
						OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.asText(regex), jsonProvider.getNodeType(flags) == JsonNodeType.NULL ? null : jsonProvider.asText(flags));
						output.emit(match(jsonProvider, p, ibytes, cindex, jsonProvider.asBoolean(test)), null);
					});
				});
			});
		};
	}

	private static class CaptureObject {
		public int offset;
		public int length;
		public @Nullable String string;
		public @Nullable String name;
	}

	/* package private */static class MatchObject {
		public int offset;
		public int length;
		public @Nullable String string;
		public List<CaptureObject> captures = new ArrayList<>();
	}

	private static <JsonNode> JsonNode captureToJson(JsonProvider<JsonNode> jsonProvider, CaptureObject capture) {
		@Var JsonNode node = jsonProvider.createObject();
		node = jsonProvider.set(node, "offset", jsonProvider.createNumber(capture.offset));
		node = jsonProvider.set(node, "length", jsonProvider.createNumber(capture.length));
		node = jsonProvider.set(node, "string", capture.string == null ? jsonProvider.createNull() : jsonProvider.createString(capture.string));
		node = jsonProvider.set(node, "name", capture.name == null ? jsonProvider.createNull() : jsonProvider.createString(capture.name));
		return node;
	}

	private static <JsonNode> JsonNode matchToJson(JsonProvider<JsonNode> jsonProvider, MatchObject obj) {
		@Var JsonNode node = jsonProvider.createObject();
		node = jsonProvider.set(node, "offset", jsonProvider.createNumber(obj.offset));
		node = jsonProvider.set(node, "length", jsonProvider.createNumber(obj.length));
		node = jsonProvider.set(node, "string", obj.string == null ? jsonProvider.createNull() : jsonProvider.createString(obj.string));
		@Var JsonNode capturesArray = jsonProvider.createArray();
		for (CaptureObject capture : obj.captures) {
			capturesArray = jsonProvider.add(capturesArray, captureToJson(jsonProvider, capture));
		}
		node = jsonProvider.set(node, "captures", capturesArray);
		return node;
	}

	private static <JsonNode> JsonNode match(JsonProvider<JsonNode> jsonProvider, OnigUtils.Pattern pattern, byte[] ibytes, int[] cindex, boolean test) {
		Matcher m = pattern.regex.matcher(ibytes);

		if (test) {
			boolean match = m.search(0, ibytes.length, Option.NONE) >= 0;
			return jsonProvider.createBoolean(match);
		} else {
			@Var JsonNode matches = jsonProvider.createArray();

			@Var int offset = 0;
			do {
				if (m.search(offset, ibytes.length, Option.NONE) < 0)
					break;

				MatchObject obj = new MatchObject();
				obj.offset = cindex[m.getBegin()];
				obj.length = cindex[m.getEnd()] - cindex[m.getBegin()];
				obj.string = new String(ibytes, m.getBegin(), m.getEnd() - m.getBegin(), StandardCharsets.UTF_8);

				// 1. regions is null when there is no capture groups
				// 2. for zero-width match, we do not include captures
				Region regions = m.getRegion();
				if (regions != null && m.getEnd() != m.getBegin()) {
					for (int i = 1; i < regions.getNumRegs(); ++i) {
						CaptureObject capture = new CaptureObject();
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
