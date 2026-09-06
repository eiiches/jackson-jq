package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.math.BigDecimal;
import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@AutoService(Function.class)
@FunctionRegistration(name = "tonumber", nargs = 0)
public class ToNumberFunction implements Function {
	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).build((scope, in, ipath, output) -> {

			JsonNodeType inType = jsonProvider.getNodeType(in);
			if (inType == JsonNodeType.NUMBER) {
				output.emit(in, UntrackedPath.getInstance());
			} else if (inType == JsonNodeType.STRING) {
				String raw = jsonProvider.getString(in);
				// jq 1.8 stopped accepting leading/trailing whitespace around an otherwise-valid
				// numeral; earlier versions trim it.
				String str = version.compareTo(Versions.JQ_1_8_0) < 0 ? raw.trim() : raw;
				try {
					// Parse via BigDecimal first to preserve the exact literal value (e.g. large
					// integers, trailing decimal zeros) instead of rounding through a double.
					output.emit(jsonProvider.createNumber(new BigDecimal(str)), UntrackedPath.getInstance());
					return;
				} catch (NumberFormatException e) {
					// Not a plain decimal numeral; check for Infinity/NaN below.
				}
				Double special = parseSpecialFloatLiteral(str, version);
				if (special != null) {
					output.emit(jsonProvider.createNumber(special), UntrackedPath.getInstance());
					return;
				}
				throw new JsonQueryException(new NumberFormatException(String.format("For input string: \"%s\"", raw)));
			} else {
				throw new JsonQueryTypeException(jsonProvider, version, "%s cannot be parsed as a number", in);
			}
		});
	}

	/**
	 * Matches jq's "Infinity"/"NaN" literal family (case-insensitive, optional leading sign),
	 * which isn't valid {@link BigDecimal} syntax. Returns {@code null} if {@code str} doesn't
	 * match either form.
	 */
	private static @Nullable Double parseSpecialFloatLiteral(String str, Version version) {
		boolean negative = str.startsWith("-");
		String body = negative || str.startsWith("+") ? str.substring(1) : str;
		if (body.equalsIgnoreCase("Infinity"))
			return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		if (isNanLiteral(body, version))
			return Double.NaN;
		return null;
	}

	/**
	 * jq 1.7 made "NaN" case-insensitive; earlier versions only accept the exact spellings "NaN"
	 * and "NAN" (e.g. lowercase "nan" errors).
	 */
	private static boolean isNanLiteral(String body, Version version) {
		if (version.compareTo(Versions.JQ_1_7) >= 0)
			return body.equalsIgnoreCase("NaN");
		return body.equals("NaN") || body.equals("NAN");
	}
}
