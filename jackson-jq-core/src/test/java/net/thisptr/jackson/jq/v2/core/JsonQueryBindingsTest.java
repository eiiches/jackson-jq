package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.json.comparator.JsonNodeComparator;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonQueryBindingsTest {
	/**
	 * Results are compared by jq value, not by JsonNode identity: the node class a literal
	 * compiles to is not what these tests are about.
	 */
	private static final Comparator<JsonNode> BY_JQ_VALUE = new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance());

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void reusesCompiledQueryWithDifferentVariableBindings() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQuery<JsonNode> query = env.compile("$value");

		assertThat(run(query, bindingsWithVariable("value", 1))).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("1")));
		assertThat(run(query, bindingsWithVariable("value", 2))).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("2")));
		assertThat(run(query, bindingsWithVariable("value", 3))).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("3")));
	}

	@Test
	public void evaluatesDefaultSupplierForEveryReference() throws Exception {
		AtomicInteger counter = new AtomicInteger();
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.defineVariable("value", () -> JSON_PROVIDER.createNumber(counter.incrementAndGet()))
				.build();

		assertThat(run(env.compile("[$value, $value]"), JsonQueryBindings.empty())).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[1,2]")));
	}

	@Test
	public void evaluatesOverrideSupplierForEveryReferenceAndThroughClosure() throws Exception {
		AtomicInteger overrideCounter = new AtomicInteger();
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQuery<JsonNode> query = env.compile("def values: [$value, $value]; values");
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setVariable("value", () -> JSON_PROVIDER.createNumber(overrideCounter.incrementAndGet()))
				.build();

		assertEquals(0, overrideCounter.get());
		assertThat(run(query, bindings)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[1,2]")));
		assertEquals(2, overrideCounter.get());
	}

	@Test
	public void doesNotEvaluateUnusedOverrideSupplier() throws Exception {
		AtomicInteger counter = new AtomicInteger();
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setVariable("value", () -> JSON_PROVIDER.createNumber(counter.incrementAndGet()))
				.build();

		run(env.compile("."), bindings);
		assertEquals(0, counter.get());
	}

	@Test
	public void reportsNullValueFromOverrideSupplier() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setVariable("value", () -> null)
				.build();

		JsonQueryException error = assertThrows(JsonQueryException.class, () -> run(env.compile("$value"), bindings));
		assertThat(error).hasMessageContaining("evaluated to null");
	}

	@Test
	public void overridesFunctionPerInvocationAndThroughClosure() throws Exception {
		FunctionSignature key = FunctionSignature.of("custom", 0);
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.declareFunction(key)
				.build();
		JsonQuery<JsonNode> query = env.compile("def wrapper: [$value, custom]; wrapper");

		JsonQueryBindings<JsonNode> firstCall = JsonQueryBindings.<JsonNode>builder()
				.setVariable("value", JSON_PROVIDER.createString("default-variable"))
				.setFunction(key, constantFunction("default-function"))
				.build();
		JsonQueryBindings<JsonNode> secondCall = JsonQueryBindings.<JsonNode>builder()
				.setVariable("value", JSON_PROVIDER.createString("override-variable"))
				.setFunction(key, constantFunction("override-function"))
				.build();

		assertThat(run(query, firstCall)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[\"default-variable\",\"default-function\"]")));
		assertThat(run(query, secondCall)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[\"override-variable\",\"override-function\"]")));
	}

	@Test
	public void rejectsUnknownOverrides() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.defineConstant("known", JSON_PROVIDER.createNull())
				.build();
		JsonQuery<JsonNode> query = env.compile("$known");

		JsonQueryException variableError = assertThrows(JsonQueryException.class,
				() -> run(query, bindingsWithVariable("unknown", 1)));
		assertThat(variableError).hasMessageContaining("$unknown");

		JsonQueryBindings<JsonNode> functionBindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(FunctionSignature.of("unknown", 0), constantFunction("unused"))
				.build();
		JsonQueryException functionError = assertThrows(JsonQueryException.class, () -> run(query, functionBindings));
		assertThat(functionError).hasMessageContaining("unknown/0");
	}

	@Test
	public void rejectsOverrideOfFixedValue() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.defineConstant("known", JSON_PROVIDER.createNumber(1))
				.build();
		JsonQuery<JsonNode> query = env.compile("$known");

		JsonQueryException error = assertThrows(JsonQueryException.class,
				() -> run(query, bindingsWithVariable("known", 2)));
		assertThat(error).hasMessageContaining("$known").hasMessageContaining("fixed value");
	}

	@Test
	public void rejectsMissingDeclaredVariable() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQuery<JsonNode> query = env.compile("$value");

		JsonQueryException error = assertThrows(JsonQueryException.class, () -> run(query, JsonQueryBindings.empty()));
		assertThat(error).hasMessageContaining("$value").hasMessageContaining("must be supplied");
	}

	@Test
	public void declaredFunctionColludingWithABuiltinStillRequiresABinding() throws Exception {
		FunctionSignature builtinCollision = FunctionSignature.of("length", 0);
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareFunction(builtinCollision)
				.build();
		JsonQuery<JsonNode> query = env.compile("length");

		JsonQueryException error = assertThrows(JsonQueryException.class, () -> run(query, JsonQueryBindings.empty()));
		assertThat(error).hasMessageContaining("length").hasMessageContaining("must be supplied");

		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(builtinCollision, constantFunction("overridden"))
				.build();
		assertThat(run(query, bindings)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("\"overridden\"")));
	}

	@Test
	public void localVariableShadowsGlobalBinding() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQuery<JsonNode> query = env.compile("10 as $value | $value");

		assertThat(run(query, bindingsWithVariable("value", 20))).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("10")));
	}

	@Test
	public void overridesOnlyDeclaredFunctionSignatureLeavingDefinedOneFixed() throws Exception {
		FunctionSignature zeroArg = FunctionSignature.of("custom", 0);
		FunctionSignature oneArg = FunctionSignature.of("custom", 1);
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareFunction(zeroArg)
				.defineFunction(oneArg, constantFunction("one"))
				.build();
		JsonQuery<JsonNode> query = env.compile("[custom, custom(.)]");
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(zeroArg, constantFunction("override"))
				.build();

		assertThat(run(query, bindings)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[\"override\",\"one\"]")));
	}

	@Test
	public void overridesVariadicFunctionUsingRegisteredSignature() throws Exception {
		FunctionSignature variadic = FunctionSignature.ofVariadic("custom");
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareFunction(variadic)
				.build();
		JsonQuery<JsonNode> query = env.compile("[custom, custom(.)]");
		JsonQueryBindings<JsonNode> bindings = JsonQueryBindings.<JsonNode>builder()
				.setFunction(variadic, constantFunction("override"))
				.build();

		assertThat(run(query, bindings)).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(MAPPER.readTree("[\"override\",\"override\"]")));
	}

	@Test
	public void isolatesBindingsAcrossConcurrentInvocations() throws Exception {
		Environment<JsonNode> env = new EnvironmentBuilder<>(JSON_PROVIDER, Versions.JQ_1_7)
				.declareVariable("value")
				.build();
		JsonQuery<JsonNode> query = env.compile("$value");
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			List<Future<List<JsonNode>>> futures = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				int value = i;
				futures.add(executor.submit(() -> run(query, bindingsWithVariable("value", value))));
			}
			for (int i = 0; i < futures.size(); i++)
				assertThat(futures.get(i).get()).usingElementComparator(BY_JQ_VALUE).isEqualTo(Arrays.asList(JSON_PROVIDER.createNumber(i)));
		} finally {
			executor.shutdownNow();
		}
	}

	private static JsonQueryBindings<JsonNode> bindingsWithVariable(String name, int value) {
		return JsonQueryBindings.<JsonNode>builder().setVariable(name, JSON_PROVIDER.createNumber(value)).build();
	}

	private static Function constantFunction(String value) {
		return new Function() {
			@Override
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jsonProvider, List<Expression<Context, N>> args, Version version) {
				return (frame, in, path, output) -> output.emit(jsonProvider.createString(value), UntrackedPath.getInstance());
			}
		};
	}

	private static List<JsonNode> run(JsonQuery<JsonNode> query, JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		List<JsonNode> result = new ArrayList<>();
		query.apply(JSON_PROVIDER.createNull(), bindings, result::add);
		return result;
	}
}
