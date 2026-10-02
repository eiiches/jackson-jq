package net.thisptr.jackson.jq.v2.test.testcase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats golden test-case files canonically.
 *
 * <pre>
 * bazelisk run //:format-test-cases -- tests/test-cases
 * bazelisk run //:format-test-cases -- --check tests/test-cases
 * </pre>
 *
 * <p>{@code --check} writes nothing, names the files that are not canonical, and exits non-zero.
 */
public final class FormatTestCases {
	public static void main(String[] args) throws Exception {
		TestCaseFiles.Invocation invocation = TestCaseFiles.parse(args, "format-test-cases");
		List<Path> offenders = new ArrayList<>();
		for (Path file : invocation.files()) {
			if (invocation.check()) {
				if (!TestCaseFormatter.isFormatted(file))
					offenders.add(file);
			} else if (TestCaseFormatter.format(file)) {
				offenders.add(file);
			}
		}
		for (Path offender : offenders)
			System.out.println((invocation.check() ? "Not formatted: " : "Formatted: ") + offender);
		System.out.printf(invocation.check() ? "%d of %d files need formatting%n" : "Formatted %d of %d files%n",
				offenders.size(), invocation.files().size());
		if (invocation.check() && !offenders.isEmpty())
			System.exit(1);
	}

	private FormatTestCases() {
	}
}
