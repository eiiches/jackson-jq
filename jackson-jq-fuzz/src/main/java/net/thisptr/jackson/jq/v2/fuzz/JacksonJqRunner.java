package net.thisptr.jackson.jq.v2.fuzz;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.internal.typecheck.ConstantTypes;
import net.thisptr.jackson.jq.v2.core.internal.typecheck.TypeMatcher;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.version.Version;
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

	enum InferenceKind {
		TYPE,
		CARDINALITY
	}

	record InferenceViolation(InferenceKind kind, String message) {
	}

	record CheckedResult(Result result, List<InferenceViolation> violations) {
	}

	private final JsonProvider<N> jsonProvider;
	private final Version jqVersion;

	public JacksonJqRunner(JsonProvider<N> jsonProvider, Version jqVersion) {
		this.jsonProvider = jsonProvider;
		this.jqVersion = jqVersion;
	}

	CheckedResult doEvaluate(JsonQuery<N> expr, N in) {
		List<JsonNode> values = new ArrayList<>();
		List<InferenceViolation> violations = new ArrayList<>();
		AtomicInteger emittedCount = new AtomicInteger();
		Type outputType = expr.getType().outputType();
		Cardinality cardinality = expr.getProperties().cardinality();
		@Var Result result;
		try {
			expr.apply(in, out -> {
				int index = emittedCount.getAndIncrement();
				Type actualType = ConstantTypes.of(jsonProvider, out);
				if (!TypeMatcher.accepts(outputType, actualType)) {
					violations.add(new InferenceViolation(InferenceKind.TYPE,
							"Output at index " + index + " has type " + actualType
									+ " but inferred output type is " + outputType + ": " + out));
				}
				try {
					values.add(MAPPER.readTree(JsonCodec.format(jsonProvider, out)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			result = new Result(values, null, null, null);
		} catch (Throwable th) {
			result = new Result(values, th, ErrorPhase.RUNTIME, th.getMessage());
		}
		if ((cardinality == Cardinality.ZERO && emittedCount.get() != 0)
				|| (cardinality == Cardinality.ONE && (result.error() == null ? emittedCount.get() != 1 : emittedCount.get() > 1))) {
			violations.add(new InferenceViolation(InferenceKind.CARDINALITY,
					"Inferred cardinality " + cardinality + " but emitted " + emittedCount.get()
							+ " value(s)" + (result.error() == null ? "" : " before a runtime error")));
		}
		return new CheckedResult(result, List.copyOf(violations));
	}

	@SuppressWarnings("deprecation")
	private static void terminateThread(Thread thread) {
		thread.stop();
	}

	@Override
	public Result evaluate(String exprText, JsonNode in, Duration timeout) throws Throwable {
		return evaluateChecked(exprText, in, timeout).result();
	}

	CheckedResult evaluateChecked(String exprText, JsonNode in, Duration timeout) throws Throwable {
		AtomicReference<CheckedResult> result = new AtomicReference<>();
		AtomicReference<Throwable> exception = new AtomicReference<>();
		Thread th = new Thread() {
			@Override
			public void run() {
				try {
					Environment<N> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, jqVersion).build();
					N nativeIn = JsonCodec.parse(jsonProvider, in.toString());
					CompileOptions options = CompileOptions.newBuilder()
							.setTypeCheckMode(TypeCheckMode.WARN)
							.setInputType(ConstantTypes.shapeOf(jsonProvider, nativeIn))
							.build();
					JsonQuery<N> jq = env.compile(exprText, options);
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
