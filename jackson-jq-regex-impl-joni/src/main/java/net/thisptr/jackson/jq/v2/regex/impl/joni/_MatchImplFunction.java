package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.google.errorprone.annotations.Var;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.regex.impl.joni.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "_match_impl", nargs = 3)
public class _MatchImplFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		Expression<Context, JsonNode> regexExpr = args.get(0);
		Expression<Context, JsonNode> flagsExpr = args.get(1);
		Expression<Context, JsonNode> testExpr = args.get(2);
		PrecompiledPatternPlan precompiled = PrecompiledPatternPlan.flagsThenRegex(jsonProvider, regexExpr, flagsExpr, true);

		if (precompiled != null) {
			return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
				Preconditions.checkInputType(jsonProvider, "_match_impl/3", in, JsonNodeType.STRING);
				byte[] ibytes = jsonProvider.getString(in).getBytes(StandardCharsets.UTF_8);
				int[] cindex = UnicodeUtils.utf8CharIndex(ibytes);

				testExpr.apply(frame, in, UntrackedPath.getInstance(), (test, opath) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
					for (OnigUtils.Pattern pattern : precompiled.patterns())
						output.emit(match(jsonProvider, pattern, ibytes, cindex, jsonProvider.getBoolean(test)), UntrackedPath.getInstance());
				});
			});
		}

		return FunctionBody.builder(args).usesInput(true).build((frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "_match_impl/3", in, JsonNodeType.STRING);
			byte[] ibytes = jsonProvider.getString(in).getBytes(StandardCharsets.UTF_8);
			int[] cindex = UnicodeUtils.utf8CharIndex(ibytes);

			testExpr.apply(frame, in, UntrackedPath.getInstance(), (test, opath) -> {
				Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 3, test, JsonNodeType.BOOLEAN);
				flagsExpr.apply(frame, in, UntrackedPath.getInstance(), (flags, opath2) -> {
					Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
					regexExpr.apply(frame, in, UntrackedPath.getInstance(), (regex, opath3) -> {
						Preconditions.checkArgumentType(jsonProvider, "_match_impl/3", 1, regex, JsonNodeType.STRING);
						OnigUtils.Pattern p = new OnigUtils.Pattern(jsonProvider.getString(regex), jsonProvider.isNull(flags) ? null : jsonProvider.getString(flags));
						output.emit(match(jsonProvider, p, ibytes, cindex, jsonProvider.getBoolean(test)), UntrackedPath.getInstance());
					});
				});
			});
		});
	}

	static class CaptureObject {
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

	/**
	 * Builds one element of the {@code captures} array.
	 *
	 * <p>The field order depends on the length of the capture, because jq's {@code f_match()}
	 * writes {@code offset} and {@code string} first for a zero-length capture but {@code offset}
	 * and {@code length} first otherwise, then writes the remaining field afterwards. Since jq
	 * objects keep insertion order, a zero-length capture comes out as
	 * {@code offset, string, length, name} and every other capture as
	 * {@code offset, length, string, name}. A capture group that did not participate in the match
	 * is reported with length 0, so it takes the former order too. jackson-jq reproduces this.
	 */
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

	private static <JsonNode> JsonNode matchToJson(JsonProvider<JsonNode> jsonProvider, MatchObject obj) {
		List<JsonNode> capturesArray = new ArrayList<>(obj.captures.size());
		for (CaptureObject capture : obj.captures) {
			capturesArray.add(captureToJson(jsonProvider, capture));
		}
		Map<String, JsonNode> node = new LinkedHashMap<>();
		node.put("offset", jsonProvider.createNumber(obj.offset));
		node.put("length", jsonProvider.createNumber(obj.length));
		node.put("string", obj.string == null ? jsonProvider.createNull() : jsonProvider.createString(obj.string));
		node.put("captures", jsonProvider.createArray(capturesArray));
		return jsonProvider.createObject(node);
	}

	private static <JsonNode> JsonNode match(JsonProvider<JsonNode> jsonProvider, OnigUtils.Pattern pattern, byte[] ibytes, int[] cindex, boolean test) {
		Matcher m = pattern.regex.matcher(ibytes);

		if (test) {
			boolean match = m.search(0, ibytes.length, Option.NONE) >= 0;
			return jsonProvider.createBoolean(match);
		} else {
			List<JsonNode> matches = new ArrayList<>();

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

				matches.add(matchToJson(jsonProvider, obj));

				if (m.getEnd() == offset) {
					++offset;
				} else {
					offset = m.getEnd();
				}
			} while (pattern.global && offset != ibytes.length);

			return jsonProvider.createArray(matches);
		}
	}
}
