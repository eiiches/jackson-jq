package net.thisptr.jackson.jq.v2.fuzz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.test.evaluator.Evaluator;

/**
 * {@link Evaluator} backed by jackson-jq's own engine, running against an arbitrary {@link JsonProvider}.
 * <p>
 * {@link Evaluator} exchanges values as Jackson2 {@link JsonNode} (the same format {@code JqRunner} uses
 * to talk to a real jq subprocess), so the input value is converted into this runner's native node type
 * before evaluation, and each output value is converted back afterwards, via the provider's own
 * {@code toString}/{@code fromString} -- which also means each provider's own jq-compatible number
 * formatting (NaN/Infinity handling, etc.) is picked up for free on the round trip.
 */
public class JacksonJqRunner<N> implements Evaluator {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final JsonProvider<N> jsonProvider;
	private final Version jqVersion;

	public JacksonJqRunner(JsonProvider<N> jsonProvider, Version jqVersion) {
		this.jsonProvider = jsonProvider;
		this.jqVersion = jqVersion;
	}

	private Result doEvaluate(JsonQuery<N> expr, N in) {
		List<JsonNode> values = new ArrayList<>();
		try {
			expr.apply(in, (out, opath) -> {
				try {
					values.add(MAPPER.readTree(jsonProvider.toString(out)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			return new Result(values, null);
		} catch (Throwable th) {
			return new Result(values, th);
		}
	}

	@SuppressWarnings("deprecation")
	private static void terminateThread(Thread thread) {
		thread.stop();
	}

	@Override
	public Result evaluate(String exprText, JsonNode in, Duration timeout) throws Throwable {
		AtomicReference<Result> result = new AtomicReference<>();
		AtomicReference<Throwable> exception = new AtomicReference<>();
		Thread th = new Thread() {
			@Override
			public void run() {
				try {
					Environment<N> env = new EnvironmentBuilder<>(jsonProvider, jqVersion).build();
					JsonQuery<N> jq = env.compile(exprText);
					N nativeIn = jsonProvider.fromString(in.toString());
					result.set(doEvaluate(jq, nativeIn));
				} catch (Throwable e) {
					exception.set(e);
				}
			}
		};
		th.start();
		th.join(timeout.toMillis());
		if (th.isAlive()) {
			terminateThread(th);
			throw new TimeoutException("timeout");
		}
		if (exception.get() != null)
			throw exception.get();
		return Objects.requireNonNull(result.get());
	}
}
