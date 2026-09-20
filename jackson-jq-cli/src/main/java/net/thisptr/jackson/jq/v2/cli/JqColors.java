package net.thisptr.jackson.jq.v2.cli;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * ANSI escape sequences used for colorizing JSON output in jq.
 * <p>
 * Supports the standard 8-part colon-delimited {@code JQ_COLORS} environment variable:
 * {@code null:false:true:numbers:strings:arrays:objects:object_keys}.
 */
final class JqColors {
	static final String RESET = "\033[0m";
	private static final Pattern VALID_CHARS = Pattern.compile("^[0-9;:]*$");

	private final String nullColor;
	private final String falseColor;
	private final String trueColor;
	private final String numberColor;
	private final String stringColor;
	private final String arrayColor;
	private final String objectColor;
	private final String keyColor;

	JqColors(String nullColor, String falseColor, String trueColor, String numberColor,
			 String stringColor, String arrayColor, String objectColor, String keyColor) {
		this.nullColor = nullColor;
		this.falseColor = falseColor;
		this.trueColor = trueColor;
		this.numberColor = numberColor;
		this.stringColor = stringColor;
		this.arrayColor = arrayColor;
		this.objectColor = objectColor;
		this.keyColor = keyColor;
	}

	String nullColor() {
		return nullColor;
	}

	String falseColor() {
		return falseColor;
	}

	String trueColor() {
		return trueColor;
	}

	String numberColor() {
		return numberColor;
	}

	String stringColor() {
		return stringColor;
	}

	String arrayColor() {
		return arrayColor;
	}

	String objectColor() {
		return objectColor;
	}

	String keyColor() {
		return keyColor;
	}

	String colorize(String color, String text) {
		return color + text + RESET;
	}

	static JqColors defaultFor(Version version) {
		String nullColor = version.compareTo(Versions.JQ_1_7) < 0 ? "\033[1;30m" : "\033[0;90m";
		return new JqColors(
				nullColor,
				"\033[0;39m",
				"\033[0;39m",
				"\033[0;39m",
				"\033[0;32m",
				"\033[1;39m",
				"\033[1;39m",
				"\033[1;34m");
	}

	static JqColors fromEnvironment(Version version, Map<String, String> env, PrintStream err) {
		JqColors defaultColors = defaultFor(version);
		String jqColors = env.get("JQ_COLORS");
		if (jqColors == null || jqColors.isEmpty()) {
			return defaultColors;
		}
		if (!VALID_CHARS.matcher(jqColors).matches()) {
			err.println("Failed to set $JQ_COLORS");
			return defaultColors;
		}

		String[] parts = jqColors.split(":", -1);
		String[] defaults = new String[] {
				defaultColors.nullColor,
				defaultColors.falseColor,
				defaultColors.trueColor,
				defaultColors.numberColor,
				defaultColors.stringColor,
				defaultColors.arrayColor,
				defaultColors.objectColor,
				defaultColors.keyColor,
		};
		String[] resolved = new String[8];
		for (@Var int i = 0; i < 8; i++) {
			if (i < parts.length) {
				resolved[i] = "\033[" + parts[i] + "m";
			} else {
				resolved[i] = defaults[i];
			}
		}

		return new JqColors(
				resolved[0],
				resolved[1],
				resolved[2],
				resolved[3],
				resolved[4],
				resolved[5],
				resolved[6],
				resolved[7]);
	}
}
