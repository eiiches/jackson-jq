package net.thisptr.jackson.jq.v2.test.evaluator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Version;

/**
 * The single, explicit source of truth for which real {@code jq} binaries this test suite
 * expects to find on the host and what they're called. Deliberately independent of
 * {@link net.thisptr.jackson.jq.v2.core.Versions#versions()} (the core library's registry of
 * versions it implements), since binary naming is a test-environment concern, not a library one.
 */
public final class JqExecutables {

	public static class JqExecutable {
		public final String executable;
		public final Version jqVersion;

		public JqExecutable(String executable, Version jqVersion) {
			this.executable = executable;
			this.jqVersion = jqVersion;
		}

		@Override
		public String toString() {
			return executable + " (jq " + jqVersion + ")";
		}
	}

	public static final List<JqExecutable> ALL = Collections.unmodifiableList(Arrays.asList(
			new JqExecutable("jq-1.5", Version.of(1, 5, 0)),
			new JqExecutable("jq-1.6", Version.of(1, 6, 0)),
			new JqExecutable("jq-1.7", Version.of(1, 7, 0)),
			new JqExecutable("jq-1.7.1", Version.of(1, 7, 1)),
			new JqExecutable("jq-1.8.0", Version.of(1, 8, 0)),
			new JqExecutable("jq-1.8.1", Version.of(1, 8, 1)),
			new JqExecutable("jq-1.8.2", Version.of(1, 8, 2))));

	public static String executableFor(Version version) {
		return ALL.stream()
				.filter(e -> e.jqVersion.equals(version))
				.findFirst()
				.map(e -> e.executable)
				.orElseThrow(() -> new IllegalArgumentException("No known jq executable for version " + version));
	}

	private JqExecutables() {
	}
}
