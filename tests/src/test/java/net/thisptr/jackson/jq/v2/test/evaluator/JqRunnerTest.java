package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JqRunnerTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String JQ = JqExecutables.ALL.get(0).executable;

	@Test
	void testJqCli() throws IOException, InterruptedException, TimeoutException {
		Evaluator.Result result = new JqRunner(JQ).evaluate("{a: (. + 1), b: 10}", MAPPER.readTree("1"), Duration.ofSeconds(1));
		assertEquals(1, result.values.size());
		assertEquals(MAPPER.readTree("{\"a\":2,\"b\":10}"), result.values.get(0));
		assertNull(result.error);
	}

	@Test
	void testJqCliError() throws IOException, InterruptedException, TimeoutException {
		Evaluator.Result result = new JqRunner(JQ).evaluate("null[]", MAPPER.readTree("null"), Duration.ofSeconds(1));
		assertEquals(0, result.values.size());
		assertNotNull(result.error);
		assertEquals("Cannot iterate over null (null)", result.error.getMessage());
	}
}
