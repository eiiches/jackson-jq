package net.thisptr.jackson.jq.v2.test.evaluator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
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
import org.jspecify.annotations.Nullable;

/**
 * {@link Evaluator} backed by a specific real {@code jq} binary. An instance is bound to one
 * executable (and thus, implicitly, one jq version) supplied by the caller; see
 * {@link JqExecutables} for the canonical list of executables this test suite knows about.
 */
public class JqRunner implements Evaluator {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final String executable;
	private final @Nullable Path moduleSearchPath;

	public JqRunner(String executable) {
		this(executable, null);
	}

	public JqRunner(String executable, @Nullable Path moduleSearchPath) {
		this.executable = executable;
		this.moduleSearchPath = moduleSearchPath;
	}

	public static boolean hasJq(String executable) {
		try {
			Process p = new ProcessBuilder(executable, "--version").start();
			p.waitFor();
			return p.exitValue() == 0;
		} catch (Throwable th) {
			return false;
		}
	}

	@Override
	public Result evaluate(String expression, JsonNode in, Duration timeout) throws IOException, InterruptedException, TimeoutException {
		List<String> args = new ArrayList<>();
		args.add(executable);
		if (moduleSearchPath != null) {
			args.add("-L");
			args.add(moduleSearchPath.toString());
		}
		args.add("-c");
		args.add(expression);
		ProcessBuilder pb = new ProcessBuilder(args);
		// Matches the ENV.PAGER jq variable AbstractJsonQueryTest sets up for the library's own
		// implementation, so `env.PAGER`/`$ENV.PAGER` test cases agree between the two.
		pb.environment().put("PAGER", "less");

		Process p = pb.start();

		try (OutputStream stdin = p.getOutputStream()) {
			stdin.write(in.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// This can happen when the process exits before we write any input, probably due to a failure to compile the expression.
		}

		if (!p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
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

		return new Result(values, error != null ? new RuntimeException(error) : null);
	}
}
