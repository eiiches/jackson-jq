package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.function.loaders.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.internal.builtins.AbstractPureJsonArgumentFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.BuiltinsFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.EmptyFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.ErrorFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.InfiniteFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.IsEmptyFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.NanFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.NowFunction;
import net.thisptr.jackson.jq.v2.core.internal.builtins.RangeFunction;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionContractTest {
	private static BindContext<JsonNode> bindContext(JsonProvider<JsonNode> jsonProvider, Version version) {
		return new BindContext<JsonNode>() {
			@Override
			public JsonProvider<JsonNode> getJsonProvider() {
				return jsonProvider;
			}

			@Override
			public Version getJqVersion() {
				return version;
			}
		};
	}

	private static <Context extends RuntimeContext> Expression<Context, JsonNode> pureExpression() {
		return new Expression<Context, JsonNode>() {
			@Override
			public boolean dependsOnInput() {
				return false;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) {
			}
		};
	}

	@Test
	public void allCoreFunctionsReturnCorrectExpressionFlags() {
		JsonProvider<JsonNode> jsonProvider = Jackson2JsonProvider.getInstance();
		for (Version version : Versions.versions()) {
			Map<FunctionSignature, Function> functions = ClassPathFunctionLoader.getInstance().getFunctions(version);
			assertThat(functions).isNotEmpty();
			for (Map.Entry<FunctionSignature, Function> entry : functions.entrySet()) {
				FunctionSignature sig = entry.getKey();
				Function fn = entry.getValue();
				int arity = sig.arity() != null ? sig.arity() : 0;
				// Test with pure dummy args
				List<Expression<RuntimeContext, JsonNode>> pureArgs = new ArrayList<>();
				for (int i = 0; i < arity; i++) {
					pureArgs.add(pureExpression());
				}
				Expression<RuntimeContext, JsonNode> expr = fn.bind(bindContext(jsonProvider, version), pureArgs);
				if (fn instanceof EmptyFunction || fn instanceof BuiltinsFunction || fn instanceof NanFunction || fn instanceof InfiniteFunction || fn instanceof RangeFunction || fn instanceof IsEmptyFunction || fn instanceof AbstractPureJsonArgumentFunction) {
					assertThat(expr.dependsOnInput())
							.as("%s/%d in %s (Pure) expr.dependsOnInput()", sig.name(), arity, version)
							.isFalse();
					assertThat(expr.dependsOnExternalState())
							.as("%s/%d in %s (Pure) expr.dependsOnExternalState()", sig.name(), arity, version)
							.isFalse();
				} else if (fn instanceof ErrorFunction) {
					if (arity == 0) {
						assertThat(expr.dependsOnInput())
								.as("error/0 in %s expr.dependsOnInput()", version)
								.isTrue();
					} else {
						assertThat(expr.dependsOnInput())
								.as("error/1 in %s expr.dependsOnInput()", version)
								.isFalse();
					}
					assertThat(expr.dependsOnExternalState())
							.as("error/%d in %s expr.dependsOnExternalState()", arity, version)
							.isFalse();
				} else if (fn instanceof NowFunction) {
					assertThat(expr.dependsOnInput())
							.as("now/%d in %s expr.dependsOnInput()", arity, version)
							.isFalse();
					assertThat(expr.dependsOnExternalState())
							.as("now/%d in %s expr.dependsOnExternalState()", arity, version)
							.isTrue();
				} else {
					assertThat(expr.dependsOnInput())
							.as("%s/%d in %s expr.dependsOnInput()", sig.name(), arity, version)
							.isTrue();
					assertThat(expr.dependsOnExternalState())
							.as("%s/%d in %s expr.dependsOnExternalState()", sig.name(), arity, version)
							.isFalse();
				}
				// If the function takes arguments, test propagating dependsOnInput and dependsOnExternalState from args
				if (arity > 0) {
					// Test arg with dependsOnInput=true
					List<Expression<RuntimeContext, JsonNode>> inputArgs = new ArrayList<>();
					for (int i = 0; i < arity; i++) {
						inputArgs.add(new Expression<RuntimeContext, JsonNode>() {
							@Override
							public boolean dependsOnInput() {
								return true;
							}

							@Override
							public boolean dependsOnExternalState() {
								return false;
							}

							@Override
							public void apply(RuntimeContext frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) {
							}
						});
					}
					Expression<RuntimeContext, JsonNode> inputExpr = fn.bind(bindContext(jsonProvider, version), inputArgs);
					assertThat(inputExpr.dependsOnInput())
							.as("%s/%d with input-dependent args in %s expr.dependsOnInput()", sig.name(), arity, version)
							.isTrue();
					// Test arg with dependsOnExternalState=true
					List<Expression<RuntimeContext, JsonNode>> externalStateArgs = new ArrayList<>();
					for (int i = 0; i < arity; i++) {
						externalStateArgs.add(new Expression<RuntimeContext, JsonNode>() {
							@Override
							public boolean dependsOnInput() {
								return false;
							}

							@Override
							public boolean dependsOnExternalState() {
								return true;
							}

							@Override
							public void apply(RuntimeContext frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) {
							}
						});
					}
					Expression<RuntimeContext, JsonNode> externalStateExpr = fn.bind(bindContext(jsonProvider, version), externalStateArgs);
					assertThat(externalStateExpr.dependsOnExternalState())
							.as("%s/%d with external-state args in %s expr.dependsOnExternalState()", sig.name(), arity, version)
							.isTrue();
				}
			}
		}
	}
}
