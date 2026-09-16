package net.thisptr.jackson.jq.v2.benchmark;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
// JMH injects every @Param field and invokes setup before calling either benchmark method.
@SuppressWarnings("NullAway")
public class JacksonJqBenchmark {
	@Param({ "" })
	public String benchmarkId = "";

	@Param({ "jackson3" })
	public String jsonProviderName = "jackson3";

	@Param({ "1.8.2" })
	public String jqVersion = "1.8.2";

	private Environment<Object> environment;
	private JsonQuery<Object> compiledQuery;
	private Object input;
	private String jqExpression;

	@Setup
	public void setup() {
		jqExpression = requiredProperty(Main.QUERY_PROPERTY);
		String jsonInput = requiredProperty(Main.INPUT_PROPERTY);
		JsonProvider<Object> jsonProvider = Main.resolveProvider(jsonProviderName);
		Version version = Main.resolveVersion(jqVersion);
		environment = EnvironmentBuilder.withDefaultLoaders(jsonProvider, version).build();
		input = jsonProvider.parse(jsonInput);
		compiledQuery = environment.compile(jqExpression);
	}

	private static String requiredProperty(String name) {
		String value = System.getProperty(name);
		if (value == null)
			throw new IllegalStateException("missing system property: " + name);
		return value;
	}

	@Benchmark
	public JsonQuery<Object> compile() {
		return environment.compile(jqExpression);
	}

	@Benchmark
	public void apply(Blackhole blackhole) {
		compiledQuery.apply(input, blackhole::consume);
	}
}
