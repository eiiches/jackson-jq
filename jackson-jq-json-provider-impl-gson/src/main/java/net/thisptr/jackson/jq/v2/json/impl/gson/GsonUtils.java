package net.thisptr.jackson.jq.v2.json.impl.gson;

import java.lang.reflect.Type;

import com.google.errorprone.annotations.Var;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Utility class for creating a Gson instance configured for jq-compatible output.
 */
public final class GsonUtils {

	private GsonUtils() {
	}

	/**
	 * Creates a Gson instance configured for jq-compatible output.
	 * This handles special cases like NaN, Infinity, and number formatting.
	 */
	public static Gson createJqCompatibleGson() {
		return new GsonBuilder()
				.serializeNulls()
				.disableHtmlEscaping()
				.registerTypeAdapter(Double.class, new JqDoubleSerializer())
				.registerTypeAdapter(double.class, new JqDoubleSerializer())
				.create();
	}

	/**
	 * Formats a double value according to jq conventions.
	 * Handles NaN, Infinity, and scientific notation formatting.
	 */
	public static String formatDouble(double val) {
		if (Double.isNaN(val)) {
			return "null";
		}
		if (Double.isInfinite(val)) {
			return val > 0 ? "1.7976931348623157e+308" : "-1.7976931348623157e+308";
		}
		// Check if the value is an integer (no fractional part)
		if (val == Math.floor(val) && !Double.isInfinite(val) && Math.abs(val) < Long.MAX_VALUE) {
			return String.valueOf((long) val);
		}
		return normalizeExponent(String.valueOf(val));
	}

	/**
	 * Normalizes the exponent notation in a number string.
	 * Converts 'e' to 'E' and adds '+' after 'E' if not present.
	 */
	private static String normalizeExponent(@Var String text) {
		// Normalize scientific notation: e -> E
		text = text.replace('e', 'E');
		// Add + after E if not present
		int eIndex = text.indexOf('E');
		if (eIndex >= 0 && eIndex + 1 < text.length()) {
			char next = text.charAt(eIndex + 1);
			if (next != '+' && next != '-') {
				text = text.substring(0, eIndex + 1) + "+" + text.substring(eIndex + 1);
			}
		}
		return text;
	}

	private static class JqDoubleSerializer implements JsonSerializer<Double> {
		@Override
		public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return JsonNull.INSTANCE;
			}
			if (Double.isNaN(src)) {
				return JsonNull.INSTANCE;
			}
			if (Double.isInfinite(src)) {
				return new JsonPrimitive(src > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE);
			}
			return new JsonPrimitive(src);
		}
	}
}
