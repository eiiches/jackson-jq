package net.thisptr.jackson.jq.v2.test.evaluator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * The single, explicit source of truth for which real {@code jq} binaries this test suite
 * expects and what they're called. Bazel developer tools use their declared jq runfiles;
 * other callers can supply a directory or resolve the names on {@code PATH}. This is independent of
 * {@link net.thisptr.jackson.jq.v2.core.version.Versions#versions()} (the core library's registry of
 * versions it implements), since binary naming is a test-environment concern, not a library one.
 */
public final class JqExecutables {
	private static final String BAZEL_JQ_BIN_DIR = "jackson_jq.test.jq_bin_dir";

	private static String bin(String executable) {
		String runfile = System.getProperty(BAZEL_JQ_BIN_DIR);
		if (runfile != null) {
			String runfiles = System.getenv("JAVA_RUNFILES");
			if (runfiles == null)
				throw new IllegalStateException("JAVA_RUNFILES is not set for Bazel jq binaries");
			return Paths.get(runfiles, "_main", runfile, executable).toString();
		}
		String binDir = System.getenv("JQ_BIN_DIR");
		return binDir == null ? executable : Paths.get(binDir, executable).toString();
	}

	public record JqExecutable(String executable, Version jqVersion) {
		@Override
		public String toString() {
			return executable + " (jq " + jqVersion + ")";
		}
	}

	private static List<JqExecutable> configuredExecutables() {
		List<JqExecutable> all = List.of(
				new JqExecutable(bin("jq-1.5"), Version.of(1, 5, 0)),
				new JqExecutable(bin("jq-1.6"), Version.of(1, 6, 0)),
				new JqExecutable(bin("jq-1.7"), Version.of(1, 7, 0)),
				new JqExecutable(bin("jq-1.7.1"), Version.of(1, 7, 1)),
				new JqExecutable(bin("jq-1.8.0"), Version.of(1, 8, 0)),
				new JqExecutable(bin("jq-1.8.1"), Version.of(1, 8, 1)),
				new JqExecutable(bin("jq-1.8.2"), Version.of(1, 8, 2)));
		if (System.getProperty(BAZEL_JQ_BIN_DIR) == null && System.getenv("JQ_BIN_DIR") == null)
			return all;

		List<JqExecutable> selected = new ArrayList<>();
		for (JqExecutable executable : all) {
			if (Files.isRegularFile(Paths.get(executable.executable())))
				selected.add(executable);
		}
		return selected;
	}

	public static final List<JqExecutable> ALL = List.copyOf(configuredExecutables());

	public static String executableFor(Version version) {
		return ALL.stream()
				.filter(e -> e.jqVersion().equals(version))
				.findFirst()
				.map(JqExecutable::executable)
				.orElseThrow(() -> new IllegalArgumentException("No known jq executable for version " + version));
	}

	private JqExecutables() {
	}
}
