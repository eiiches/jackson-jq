package net.thisptr.jackson.jq.benchmark;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.loaders.BuiltinModuleLoader;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class JacksonJqBenchmark {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Param({ "" })
	public String jqExpression = "";

	@Param({ "" })
	public String jsonInput = "";

	@Param({ "1.7" })
	public String jqVersion = "1.7";

	private Version version;
	private net.thisptr.jackson.jq.Scope rootScope;
	private JsonQuery compiledQuery;
	private JsonNode input;

	@Setup
	public void setup() throws Exception {
		version = Main.resolveVersion(jqVersion);
		rootScope = net.thisptr.jackson.jq.Scope.newEmptyScope();
		BuiltinFunctionLoader.getInstance().loadFunctions(version, rootScope);
		rootScope.setModuleLoader(BuiltinModuleLoader.getInstance());
		input = MAPPER.readTree(jsonInput);
		compiledQuery = JsonQuery.compile(jqExpression, version);
	}

	@Benchmark
	public JsonQuery compile() throws JsonQueryException {
		return JsonQuery.compile(jqExpression, version);
	}

	@Benchmark
	public void apply(final Blackhole blackhole) throws JsonQueryException {
		compiledQuery.apply(rootScope, input, blackhole::consume);
	}
}
