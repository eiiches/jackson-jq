package net.thisptr.jackson.jq.v2.test.testcase;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Spells a jq version the way the golden YAML writes it in a {@code v:} range.
 *
 * <p>{@link Version#toString()} always prints three components, while the test cases write each
 * release as it is named -- {@code 1.7} but {@code 1.7.1}, {@code 1.8.0} but not {@code 1.8}. A
 * regenerated range has to read like a hand-written one, so the spellings live here. A version
 * with no spelling is an error rather than a guess: configuring one in {@code Versions} means
 * adding it below.
 */
public final class VersionSpelling {
	private static final List<String> SPELLINGS = List.of("1.5", "1.6", "1.7", "1.7.1", "1.8.0", "1.8.1", "1.8.2");

	public static String of(Version version) {
		for (String spelling : SPELLINGS) {
			if (Version.valueOf(spelling).equals(version))
				return spelling;
		}
		throw new IllegalArgumentException("no YAML spelling for jq " + version + "; add it to VersionSpelling");
	}

	private VersionSpelling() {
	}
}
