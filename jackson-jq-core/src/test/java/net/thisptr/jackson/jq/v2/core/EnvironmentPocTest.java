package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnvironmentPocTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonProvider<JsonNode> jsonProvider = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void testAddFunctionAndExecute() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		env.addFunctionFactory(FunctionNameAndArity.of("examplefn", 1), (args, version) -> new net.thisptr.jackson.jq.v2.spi.Function() {
			@Override
			public <N> void apply(N in, @Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output) throws JsonQueryException {
				String text = jsonProvider.asText((JsonNode) in);
				output.emit((N) jsonProvider.createString("hello:" + text), path);
			}
		});

		CompiledQuery<JsonNode> q = env.compile("examplefn(.)");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("\"world\""), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals("hello:world", out.get(0).asText());
	}

	@Test
	public void testAddVariableAndExecute() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);
		env.addVariable("var", () -> jsonProvider.createNumber(42));

		CompiledQuery<JsonNode> q = env.compile("$var");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("{}"), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals(42, out.get(0).asInt());
	}

	@Test
	public void testFunctionFactoryWithConstantPreEvaluation() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		AtomicBoolean preCompiled = new AtomicBoolean(false);

		FunctionFactory testFactory = (List<Expression> args, Version ver) -> {
			Expression patternExpr = args.get(0);
			JsonNode constantVal = patternExpr.evaluateConstantExpr(jsonProvider);
			if (constantVal != null) {
				preCompiled.set(true);
				Pattern pattern = Pattern.compile(jsonProvider.asText(constantVal));
				return new net.thisptr.jackson.jq.v2.spi.Function() {
					@Override
					public <N> void apply(N in, @Nullable Path<N> path, net.thisptr.jackson.jq.v2.spi.PathOutput<N> output) throws JsonQueryException {
						boolean matches = pattern.matcher(jsonProvider.asText((JsonNode) in)).find();
						output.emit((N) jsonProvider.createBoolean(matches), path);
					}
				};
			} else {
				throw new IllegalArgumentException("Expected constant pattern argument in PoC");
			}
		};

		env.addFunctionFactory(FunctionNameAndArity.of("test", 1), testFactory);

		// Compile query with constant pattern argument "foo.*bar"
		CompiledQuery<JsonNode> q = env.compile("test(\"foo.*bar\")");

		assertTrue(preCompiled.get(), "Regex pattern should be pre-compiled at query compile time!");

		List<JsonNode> out1 = new ArrayList<>();
		q.apply(MAPPER.readTree("\"foozbar\""), (outNode, path) -> out1.add(outNode));
		assertEquals(1, out1.size());
		assertTrue(out1.get(0).asBoolean());

		List<JsonNode> out2 = new ArrayList<>();
		q.apply(MAPPER.readTree("\"hello\""), (outNode, path) -> out2.add(outNode));
		assertEquals(1, out2.size());
		assertTrue(!out2.get(0).asBoolean());
	}

	@Test
	public void testUndefinedFunctionThrowsAtCompileTime() {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("nonExistentFunc(.)");
		});

		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("nonExistentFunc/1 does not exist"));
	}

	@Test
	public void testUndefinedVariableThrowsAtCompileTime() {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		JsonQueryException ex = assertThrows(JsonQueryException.class, () -> {
			env.compile("$undefinedVar");
		});

		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("Variable $undefinedVar is not defined"));
	}

	@Test
	public void testLocalAstVariableResolution() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		CompiledQuery<JsonNode> q = env.compile(". as $x | $x");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("123"), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals(123, out.get(0).asInt());
	}
}
