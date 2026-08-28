package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

import static org.assertj.core.api.Assertions.assertThat;

public class DebugModuleTest {
	@Test
	public void emitsScopeAndInputInformation() throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::debug_scope");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		assertThat(results.get(0).has("scope")).isTrue();
		assertThat(results.get(0).has("input")).isTrue();
	}

	@Test
	public void exposesFunctions() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("debug_scope", 0),
				FunctionSignature.of("debug_expr", 1),
				FunctionSignature.of("dump_expr", 1));
	}

	@Test
	public void functionContract() {
		ModuleImpl module = new ModuleImpl();
		module.getFunctions().forEach((signature, fn) -> {
			Expression<Object, JsonNode> expr = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), dummyArgs(signature.arity()), Versions.JQ_1_6);
			assertThat(expr.dependsOnExternalState()).isFalse();
		});
	}

	@Test
	public void debugScopeDependsOnFlags() {
		ModuleImpl module = new ModuleImpl();
		Function fn = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("debug_scope", 0)));
		Expression<Object, JsonNode> expr = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), dummyArgs(0), Versions.JQ_1_6);
		assertThat(expr.dependsOnInput()).isTrue();
	}

	@Test
	public void debugExprIsItselfConstant() {
		ModuleImpl module = new ModuleImpl();
		Function fn = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("debug_expr", 1)));
		Expression<Object, JsonNode> expr = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), dummyArgs(1), Versions.JQ_1_6);
		assertThat(expr.dependsOnInput()).isFalse();
		assertThat(expr.dependsOnExternalState()).isFalse();
	}

	@Test
	public void debugExprReportsConstantExpression() throws JsonQueryException {
		JsonNode result = debugExpr("1+1");
		assertThat(result.get("depends_on_input").asBoolean()).isFalse();
		assertThat(result.get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(result.has("depends_on_variables")).isFalse();
	}

	@Test
	public void debugExprReportsInputDependency() throws JsonQueryException {
		JsonNode result = debugExpr(".");
		assertThat(result.get("depends_on_input").asBoolean()).isTrue();
		assertThat(result.get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(result.has("depends_on_variables")).isFalse();
	}

	@Test
	public void debugExprOmitsVariableDependency() throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.defineVariable("x", () -> Jackson2JsonProviderImpl.getInstance().createNumber(1))
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::debug_expr($x)");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		assertThat(results.get(0).get("depends_on_input").asBoolean()).isFalse();
		assertThat(results.get(0).get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(results.get(0).has("depends_on_variables")).isFalse();
	}

	@Test
	public void debugExprNeverEvaluatesItsArgument() throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::debug_expr(error(\"boom\"))");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
	}

	@Test
	public void dumpExprReportsNestedExpressionsAndDependencies() throws JsonQueryException {
		JsonNode result = dumpExpr("1 + .");
		assertThat(result.get("object").asText()).matches("net\\.thisptr\\..+@\\p{XDigit}+");
		assertThat(result.get("class").asText()).endsWith(".PlusExpression");
		assertThat(result.get("cardinality").asText()).isEqualTo("one");
		assertThat(result.get("depends_on_input").asBoolean()).isTrue();
		assertThat(result.get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(result.has("depends_on_variables")).isFalse();
		JsonNode lhs = result.get("fields").get("lhs");
		assertThat(lhs.get("class").asText()).endsWith(".LongLiteral");
		assertThat(lhs.get("cardinality").asText()).isEqualTo("one");
		assertThat(lhs.get("depends_on_input").asBoolean()).isFalse();
		assertThat(lhs.get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(lhs.has("depends_on_variables")).isFalse();
		JsonNode rhs = result.get("fields").get("rhs");
		assertThat(rhs.get("class").asText()).endsWith(".ThisObject");
		assertThat(rhs.get("cardinality").asText()).isEqualTo("one");
		assertThat(rhs.get("depends_on_input").asBoolean()).isTrue();
		assertThat(rhs.get("depends_on_external_state").asBoolean()).isFalse();
		assertThat(rhs.has("depends_on_variables")).isFalse();
		JsonNode provider = result.get("fields").get("jsonProvider");
		assertThat(provider.get("object").asText()).startsWith(provider.get("class").asText() + "@");
		assertThat(provider.has("value")).isTrue();
		assertThat(provider.has("fields")).isFalse();
	}

	@Test
	public void dumpExprReportsConservativeCardinality() throws JsonQueryException {
		assertThat(dumpExpr("empty").get("cardinality").asText()).isEqualTo("zero");
		assertThat(dumpExpr("[.[]]").get("cardinality").asText()).isEqualTo("one");
		assertThat(dumpExpr(".[]").get("cardinality").asText()).isEqualTo("unknown");
	}

	@Test
	public void dumpExprRecursesIntoContainers() throws JsonQueryException {
		JsonNode result = dumpExpr("(1, .)");
		JsonNode container = result.get("fields").get("qs");
		assertThat(container.get("object").asText()).startsWith(container.get("class").asText() + "@");
		JsonNode expressions = container.get("elements");
		assertThat(expressions.isArray()).isTrue();
		assertThat(expressions).hasSize(2);
		assertThat(expressions.get(0).get("class").asText()).endsWith(".LongLiteral");
		assertThat(expressions.get(1).get("class").asText()).endsWith(".ThisObject");
	}

	@Test
	public void dumpExprUsesToStringAndReferencesForCycles() throws JsonQueryException {
		CyclicExpression argument = new CyclicExpression();
		assertThat(argument.opaque.toString()).isEqualTo("opaque-value");
		assertThat(argument.self).isSameAs(argument);
		assertThat(argument.values).containsExactly(argument, "scalar-value");
		assertThat(argument.mapping).hasSize(1);
		JsonNode result = dumpExpression(argument);
		JsonNode fields = result.get("fields");
		String rootIdentity = result.get("object").asText();
		assertThat(fields.get("opaque").get("value").asText()).isEqualTo("opaque-value");
		assertThat(fields.get("self").get("$ref").asText()).isEqualTo(rootIdentity);
		assertThat(fields.get("values").get("elements").get(0).get("$ref").asText()).isEqualTo(rootIdentity);
		assertThat(fields.get("values").get("elements").get(1).asText()).isEqualTo("scalar-value");
		JsonNode mapEntry = fields.get("mapping").get("entries").get(0);
		assertThat(mapEntry.get("key").get("value").asText()).isEqualTo("map-key");
		assertThat(mapEntry.get("value").get("$ref").asText()).isEqualTo(rootIdentity);
	}

	@Test
	public void dumpExprReflectsPipeComponentsAndFunctionDefinitions() throws JsonQueryException {
		JsonNode result = dumpExpr(". as $a | def f: $a; f");
		JsonNode components = result.get("fields").get("components");
		assertThat(components.get("object").asText()).startsWith(components.get("class").asText() + "@");
		JsonNode firstComponent = components.get("elements").get(0);
		assertThat(firstComponent.get("class").asText()).endsWith(".AssignPipeComponent");
		assertThat(firstComponent.get("fields").get("expr").get("class").asText()).endsWith(".ThisObject");
		JsonNode secondComponent = components.get("elements").get(1);
		assertThat(secondComponent.get("class").asText()).endsWith(".TransformPipeComponent");
		JsonNode semicolon = secondComponent.get("fields").get("expr");
		assertThat(semicolon.get("class").asText()).endsWith(".SemicolonOperator");
		JsonNode functionDefinition = semicolon.get("fields").get("qs").get("elements").get(0);
		assertThat(functionDefinition.get("class").asText()).endsWith(".ResolvedFunctionDefinition");
		assertThat(functionDefinition.get("fields").get("resolvedBody").get("class").asText()).endsWith("VariableAccess");
	}

	@Test
	public void dumpExprNeverEvaluatesItsArgument() throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::dump_expr(error(\"boom\"))");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
	}

	private static JsonNode debugExpr(String filter) throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::debug_expr(" + filter + ")");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		return results.get(0);
	}

	private static JsonNode dumpExpr(String filter) throws JsonQueryException {
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/debug\" as debug; debug::dump_expr(" + filter + ")");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		return results.get(0);
	}

	private static JsonNode dumpExpression(Expression<?, JsonNode> argument) throws JsonQueryException {
		ModuleImpl module = new ModuleImpl();
		Function fn = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("dump_expr", 1)));
		@SuppressWarnings("unchecked")
		Expression<Object, JsonNode> castArg = (Expression<Object, JsonNode>) argument;
		Expression<Object, JsonNode> expression = fn.bindArguments(Jackson2JsonProviderImpl.getInstance(), Arrays.asList(castArg), Versions.JQ_1_6);
		List<JsonNode> results = new ArrayList<>();
		expression.apply(new Object(), Jackson2JsonProviderImpl.getInstance().createNull(), UntrackedPath.getInstance(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		return results.get(0);
	}

	private static <Context> List<Expression<Context, JsonNode>> dummyArgs(@Nullable Integer arity) {
		int n = arity == null ? 0 : arity;
		List<Expression<Context, JsonNode>> args = new ArrayList<>();
		for (int i = 0; i < n; ++i) {
			args.add(new Expression<Context, JsonNode>() {
				@Override
				public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) {
					throw new UnsupportedOperationException();
				}
			});
		}
		return args;
	}

	private static final class CyclicExpression implements Expression<Object, JsonNode> {
		private final Map<Object, Object> mapping = Collections.singletonMap(new Object() {
			@Override
			public String toString() {
				return "map-key";
			}
		}, this);
		private final Object opaque = new Object() {
			@Override
			public String toString() {
				return "opaque-value";
			}
		};
		private final Expression<Object, JsonNode> self = this;
		private final List<Object> values = Arrays.asList(this, "scalar-value");

		@Override
		public boolean dependsOnInput() {
			return false;
		}

		@Override
		public boolean dependsOnExternalState() {
			return false;
		}

		@Override
		public void apply(Object frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) {
			throw new AssertionError("The argument must not be evaluated");
		}
	}
}
