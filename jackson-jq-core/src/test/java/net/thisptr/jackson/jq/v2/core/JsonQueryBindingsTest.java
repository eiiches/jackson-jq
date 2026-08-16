package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonQueryBindingsTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void reusesCompiledQueryWithDifferentVariableBindings() throws Exception {
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("value", JSON_PROVIDER.createNumber(1));
		JsonQuery<JsonNode> query = env.compile("$value");

		assertEquals(Arrays.asList(MAPPER.readTree("1")), run(query, JsonQueryBindings.empty()));
		assertEquals(Arrays.asList(MAPPER.readTree("2")), run(query, bindingsWithVariable("value", 2)));
		assertEquals(Arrays.asList(MAPPER.readTree("3")), run(query, bindingsWithVariable("value", 3)));
	}

	@Test
	public void evaluatesDefaultSupplierForEveryReference() throws Exception {
		AtomicInteger counter = new AtomicInteger();
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("value", () -> JSON_PROVIDER.createNumber(counter.incrementAndGet()));

		assertEquals(Arrays.asList(MAPPER.readTree("[1,2]")), run(env.compile("[$value, $value]"), JsonQueryBindings.empty()));
	}

	@Test
	public void overridesFunctionFactoryPerInvocationAndThroughClosure() throws Exception {
		FunctionNameAndArity key = FunctionNameAndArity.of("custom", 0);
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("value", JSON_PROVIDER.createString("default-variable"));
		env.addFunctionFactory(key, constantFunction("default-function"));
		JsonQuery<JsonNode> query = env.compile("def wrapper: [$value, custom]; wrapper");

		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.addVariable("value", JSON_PROVIDER.createString("override-variable"))
				.addFunctionFactory(key, constantFunction("override-function"))
				.build();

		assertEquals(Arrays.asList(MAPPER.readTree("[\"default-variable\",\"default-function\"]")), run(query, JsonQueryBindings.empty()));
		assertEquals(Arrays.asList(MAPPER.readTree("[\"override-variable\",\"override-function\"]")), run(query, bindings));
	}

	@Test
	public void rejectsUnknownOverrides() throws Exception {
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("known", JSON_PROVIDER.createNull());
		JsonQuery<JsonNode> query = env.compile("$known");

		JsonQueryException variableError = assertThrows(JsonQueryException.class,
				() -> run(query, bindingsWithVariable("unknown", 1)));
		assertTrue(variableError.getMessage().contains("$unknown"));

		JsonQueryBindings<JsonNode> functionBindings = JsonQueryBindings.<JsonNode>builder()
				.addFunctionFactory(FunctionNameAndArity.of("unknown", 0), constantFunction("unused"))
				.build();
		JsonQueryException functionError = assertThrows(JsonQueryException.class, () -> run(query, functionBindings));
		assertTrue(functionError.getMessage().contains("unknown/0"));
	}

	@Test
	public void localVariableShadowsGlobalBinding() throws Exception {
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("value", JSON_PROVIDER.createNumber(1));
		JsonQuery<JsonNode> query = env.compile("10 as $value | $value");

		assertEquals(Arrays.asList(MAPPER.readTree("10")), run(query, bindingsWithVariable("value", 20)));
	}

	@Test
	public void overridesOnlyTheMatchingFunctionSignature() throws Exception {
		FunctionNameAndArity zeroArg = FunctionNameAndArity.of("custom", 0);
		FunctionNameAndArity oneArg = FunctionNameAndArity.of("custom", 1);
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addFunctionFactory(zeroArg, constantFunction("zero"));
		env.addFunctionFactory(oneArg, constantFunction("one"));
		JsonQuery<JsonNode> query = env.compile("[custom, custom(.)]");
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.addFunctionFactory(zeroArg, constantFunction("override"))
				.build();

		assertEquals(Arrays.asList(MAPPER.readTree("[\"override\",\"one\"]")), run(query, bindings));
	}

	@Test
	public void overridesVariadicFunctionUsingRegisteredSignature() throws Exception {
		FunctionNameAndArity variadic = FunctionNameAndArity.of("custom", 0).withArity(null);
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addFunctionFactory(variadic, constantFunction("default"));
		JsonQuery<JsonNode> query = env.compile("[custom, custom(.)]");
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.addFunctionFactory(variadic, constantFunction("override"))
				.build();

		assertEquals(Arrays.asList(MAPPER.readTree("[\"override\",\"override\"]")), run(query, bindings));
	}

	@Test
	public void isolatesBindingsAcrossConcurrentInvocations() throws Exception {
		Environment<JsonNode> env = new Environment<>(JSON_PROVIDER, Versions.JQ_1_7);
		env.addVariable("value", JSON_PROVIDER.createNumber(-1));
		JsonQuery<JsonNode> query = env.compile("$value");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<List<JsonNode>>> futures = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				int value = i;
				futures.add(executor.submit(() -> run(query, bindingsWithVariable("value", value))));
			}
			for (int i = 0; i < futures.size(); i++)
				assertEquals(Arrays.asList(JSON_PROVIDER.createNumber(i)), futures.get(i).get());
		} finally {
			executor.shutdownNow();
		}
	}

	private static JsonQueryBindings<JsonNode> bindingsWithVariable(String name, int value) {
		return JsonQueryBindings.<JsonNode>builder().addVariable(name, JSON_PROVIDER.createNumber(value)).build();
	}

	private static FunctionFactory constantFunction(String value) {
		return new FunctionFactory() {
			@Override
			public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression<N>> args, Version version) {
				return (frame, in, path, output) -> output.emit(jsonProvider.createString(value), null);
			}
		};
	}

	private static List<JsonNode> run(JsonQuery<JsonNode> query, JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		List<JsonNode> result = new ArrayList<>();
		query.apply(JSON_PROVIDER.createNull(), bindings, (value, path) -> result.add(value));
		return result;
	}
}
