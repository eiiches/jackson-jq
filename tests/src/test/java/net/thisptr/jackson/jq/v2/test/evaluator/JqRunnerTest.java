package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JqRunnerTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String JQ = JqExecutables.ALL.get(0).executable();

	@Test
	void testJqCli() throws IOException, InterruptedException, TimeoutException {
		Evaluator.Result result = new JqRunner(JQ).evaluate("{a: (. + 1), b: 10}", MAPPER.readTree("1"), Duration.ofSeconds(1));
		assertThat(result.values()).hasSize(1);
		assertThat(result.values().get(0)).isEqualTo(MAPPER.readTree("{\"a\":2,\"b\":10}"));
		assertThat(result.error()).isNull();
	}

	@Test
	void testJqCliError() throws IOException, InterruptedException, TimeoutException {
		Evaluator.Result result = new JqRunner(JQ).evaluate("null[]", MAPPER.readTree("null"), Duration.ofSeconds(1));
		assertThat(result.values()).isEmpty();
		assertThat(result.error()).isNotNull().hasMessage("Cannot iterate over null (null)");
	}
}
