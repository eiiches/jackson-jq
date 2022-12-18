package net.thisptr.jackson.jq.test.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class TrueJqEvaluator implements Evaluator {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static String executable(final Version version) {
		return Versions.LATEST.equals(version) ? "jq" : "jq-" + version.toString();
	}

	public static boolean hasJq(final Version version) {
		try {
			final Process p = Runtime.getRuntime().exec(new String[] {
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
	public Result evaluate(final String expr, final JsonNode in, final Version version, final long timeout) throws IOException, InterruptedException, TimeoutException {
		final ProcessBuilder pb = new ProcessBuilder(executable(version), "-c", expr);
		pb.environment().put("PAGER", "less");
		final Process p = pb.start();

		try (final OutputStream stdin = p.getOutputStream()) {
			stdin.write(in.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// This can happen when the process exits before we write any input, probably due to a failure to compile the expression.
		}

		if (!p.waitFor(timeout, TimeUnit.MILLISECONDS)) {
			p.destroyForcibly();
			throw new TimeoutException("timeout");
		}

		final List<JsonNode> values = new ArrayList<>();
		try (final InputStream stdout = p.getInputStream()) {
			final JsonParser parser = MAPPER.getFactory().createParser(ByteStreams.toByteArray(stdout));
			final MappingIterator<JsonNode> iter = MAPPER.readValues(parser, JsonNode.class);
			while (iter.hasNextValue()) {
				values.add(iter.nextValue());
			}
		}

		String error = null;
		if (p.exitValue() != 0) {
			try (final InputStream stderr = p.getErrorStream()) {
				final String message = new String(ByteStreams.toByteArray(stderr), StandardCharsets.UTF_8);
				final String[] tokens = message.trim().split(": ", 3);
				if (tokens.length != 3)
					throw new IllegalStateException("invalid jq error format: " + message);
				error = tokens[2];
			}
		}

		return new Result(values, error != null ? new JsonQueryException(error) : null);
	}

	@Test
	void testJqCli() throws JsonQueryException, IOException, InterruptedException, TimeoutException {
		final Result result = evaluate("{a: (. + 1), b: 10}", MAPPER.readTree("1"), Versions.JQ_1_5, 1000L);
		assertEquals(1, result.values.size());
		assertEquals(MAPPER.readTree("{\"a\":2,\"b\":10}"), result.values.get(0));
		assertNull(result.error);
	}

	@Test
	void testJqCliError() throws JsonQueryException, IOException, InterruptedException, TimeoutException {
		final Result result = evaluate("null[]", MAPPER.readTree("null"), Versions.JQ_1_5, 1000L);
		assertEquals(0, result.values.size());
		assertNotNull(result.error);
		assertEquals("Cannot iterate over null (null)", result.error.getMessage());
	}
}
