package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

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

		env.addFunction(FunctionNameAndArity.of("examplefn", 1), new Function() {
			@Override
			public <N> Expression<N> bindArguments(JsonProvider<N> provider, List<Expression<N>> args, Version version) {
				return (scope, in, path, output, ignoredRequirePath) -> {
					String text = provider.asText(in);
					output.emit(provider.createString("hello:" + text), path);
				};
			}
		});

		JsonQuery<JsonNode> q = env.compile("examplefn(.)");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("\"world\""), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals("hello:world", out.get(0).asText());
	}

	@Test
	public void testAddVariableAndExecute() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);
		env.addVariable("var", () -> jsonProvider.createNumber(42));

		JsonQuery<JsonNode> q = env.compile("$var");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("{}"), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals(42, out.get(0).asInt());
	}

	@Test
	public void testFunctionWithConstantPreEvaluation() throws Exception {
		Environment<JsonNode> env = new Environment<>(jsonProvider, Versions.JQ_1_7);

		AtomicBoolean preCompiled = new AtomicBoolean(false);

		Function testFactory = new Function() {
			@Override
			public <N> Expression<N> bindArguments(JsonProvider<N> provider, List<Expression<N>> args, Version ver) {
				Expression<N> patternExpr = args.get(0);
				N constantVal = patternExpr.evaluateConstantExpr();
				if (constantVal != null) {
					preCompiled.set(true);
					Pattern pattern = Pattern.compile(provider.asText(constantVal));
					return (scope, in, path, output, ignoredRequirePath) -> {
						boolean matches = pattern.matcher(provider.asText(in)).find();
						output.emit(provider.createBoolean(matches), path);
					};
				} else {
					throw new IllegalArgumentException("Expected constant pattern argument in PoC");
				}
			}
		};

		env.addFunction(FunctionNameAndArity.of("test", 1), testFactory);

		// Compile query with constant pattern argument "foo.*bar"
		JsonQuery<JsonNode> q = env.compile("test(\"foo.*bar\")");

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

		JsonQuery<JsonNode> q = env.compile(". as $x | $x");

		List<JsonNode> out = new ArrayList<>();
		q.apply(MAPPER.readTree("123"), (outNode, path) -> out.add(outNode));

		assertEquals(1, out.size());
		assertEquals(123, out.get(0).asInt());
	}
}
