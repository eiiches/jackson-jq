package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TrueJqEvaluator implements Evaluator {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static String executable(Version version) {
		return "jq-" + version.toString();
	}

	public static boolean hasJq(Version version) {
		try {
			Process p = Runtime.getRuntime().exec(new String[] {
					executable(version),
					"--version"
			});
			p.waitFor();
			return p.exitValue() == 0;
		} catch (Throwable th) {
			return false;
		}
	}

	@Override
	public Result evaluate(String expr, JsonNode in, Version version, long timeout) throws IOException, InterruptedException, TimeoutException {
		ProcessBuilder pb = new ProcessBuilder(executable(version), "-c", expr);
		Process p = pb.start();

		try (OutputStream stdin = p.getOutputStream()) {
			stdin.write(in.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// This can happen when the process exits before we write any input, probably due to a failure to compile the expression.
		}

		if (!p.waitFor(timeout, TimeUnit.MILLISECONDS)) {
			p.destroyForcibly();
			throw new TimeoutException("timeout");
		}

		List<JsonNode> values = new ArrayList<>();
		try (InputStream stdout = p.getInputStream()) {
			JsonParser parser = MAPPER.getFactory().createParser(ByteStreams.toByteArray(stdout));
			MappingIterator<JsonNode> iter = MAPPER.readValues(parser, JsonNode.class);
			while (iter.hasNextValue()) {
				values.add(iter.nextValue());
			}
		}

		@Var String error = null;
		if (p.exitValue() != 0) {
			try (InputStream stderr = p.getErrorStream()) {
				String message = new String(ByteStreams.toByteArray(stderr), StandardCharsets.UTF_8);
				String[] tokens = message.trim().split(": ", 3);
				if (tokens.length != 3)
					throw new IllegalStateException("invalid jq error format: " + message);
				error = tokens[2];
			}
		}

		return new Result(values, error != null ? new JsonQueryException(error) : null);
	}

	@Test
	void testJqCli() throws JsonQueryException, IOException, InterruptedException, TimeoutException {
		Result result = evaluate("{a: (. + 1), b: 10}", MAPPER.readTree("1"), Versions.JQ_1_5, 1000L);
		assertEquals(1, result.values.size());
		assertEquals(MAPPER.readTree("{\"a\":2,\"b\":10}"), result.values.get(0));
		assertNull(result.error);
	}

	@Test
	void testJqCliError() throws JsonQueryException, IOException, InterruptedException, TimeoutException {
		Result result = evaluate("null[]", MAPPER.readTree("null"), Versions.JQ_1_5, 1000L);
		assertEquals(0, result.values.size());
		assertNotNull(result.error);
		assertEquals("Cannot iterate over null (null)", result.error.getMessage());
	}
}
