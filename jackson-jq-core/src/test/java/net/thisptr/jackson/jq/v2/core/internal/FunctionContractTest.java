package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.internal.functions.BuiltinsFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.EmptyFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.ErrorFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.InfiniteFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.NanFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.NowFunction;
import net.thisptr.jackson.jq.v2.core.internal.functions.RangeFunction;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.path.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionContractTest {

	private static <Context> Expression<Context, JsonNode> pureExpression() {
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
			public void apply(Context context, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) {
			}
		};
	}

	@Test
	public void allCoreFunctionsReturnCorrectExpressionFlags() {
		JsonProvider<JsonNode> jsonProvider = Jackson2JsonProviderImpl.getInstance();
		for (Version version : Versions.versions()) {
			Map<FunctionSignature, Function> functions = ClassPathFunctionLoader.getInstance().getFunctions(version);
			assertThat(functions).isNotEmpty();

			for (Map.Entry<FunctionSignature, Function> entry : functions.entrySet()) {
				FunctionSignature sig = entry.getKey();
				Function fn = entry.getValue();
				int arity = sig.arity() != null ? sig.arity() : 0;

				// Test with pure dummy args
				List<Expression<Object, JsonNode>> pureArgs = new ArrayList<>();
				for (int i = 0; i < arity; i++) {
					pureArgs.add(pureExpression());
				}

				Expression<Object, JsonNode> expr = fn.bindArguments(jsonProvider, pureArgs, version);

				if (fn instanceof EmptyFunction || fn instanceof BuiltinsFunction || fn instanceof NanFunction || fn instanceof InfiniteFunction || fn instanceof RangeFunction || fn instanceof PureJsonArgumentFunction) {
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
					List<Expression<Object, JsonNode>> inputArgs = new ArrayList<>();
					for (int i = 0; i < arity; i++) {
						inputArgs.add(new Expression<Object, JsonNode>() {
							@Override
							public boolean dependsOnInput() {
								return true;
							}

							@Override
							public boolean dependsOnExternalState() {
								return false;
							}

							@Override
							public void apply(Object frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) {
							}
						});
					}
					Expression<Object, JsonNode> inputExpr = fn.bindArguments(jsonProvider, inputArgs, version);
					assertThat(inputExpr.dependsOnInput())
							.as("%s/%d with input-dependent args in %s expr.dependsOnInput()", sig.name(), arity, version)
							.isTrue();

					// Test arg with dependsOnExternalState=true
					List<Expression<Object, JsonNode>> externalStateArgs = new ArrayList<>();
					for (int i = 0; i < arity; i++) {
						externalStateArgs.add(new Expression<Object, JsonNode>() {
							@Override
							public boolean dependsOnInput() {
								return false;
							}

							@Override
							public boolean dependsOnExternalState() {
								return true;
							}

							@Override
							public void apply(Object frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) {
							}
						});
					}
					Expression<Object, JsonNode> externalStateExpr = fn.bindArguments(jsonProvider, externalStateArgs, version);
					assertThat(externalStateExpr.dependsOnExternalState())
							.as("%s/%d with external-state args in %s expr.dependsOnExternalState()", sig.name(), arity, version)
							.isTrue();
				}
			}
		}
	}
}
