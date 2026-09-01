package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the assumptions the rest of the test suite makes about the real {@code jq} CLI
 * binaries installed on the host: that each expected executable is on {@code PATH} and
 * reports the version we think it is. Unlike {@link JqRunner#hasJq}, which is used to
 * silently skip comparison against a missing binary, this test fails loudly so drift between
 * the documented/expected set of installed jq versions and reality is caught.
 */
@Order(1)
public class JqExecutablesTest {
	static List<JqExecutables.JqExecutable> executables() {
		return JqExecutables.ALL;
	}

	@ParameterizedTest
	@MethodSource("executables")
	void verifyExecutableExists(JqExecutables.JqExecutable ev) throws IOException, InterruptedException {
		assertThat(JqRunner.hasJq(ev.executable)).withFailMessage("failed to run `%s --version`", ev.executable).isTrue();
	}
}
