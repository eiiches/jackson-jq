package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

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

public class ModuleDiscoveryTest {
	private static final BindContext<JsonNode> BIND_CONTEXT = new BindContext<JsonNode>() {
		@Override
		public JsonProvider<JsonNode> getJsonProvider() {
			return Jackson2JsonProvider.getInstance();
		}

		@Override
		public Version getJqVersion() {
			return Versions.JQ_1_6;
		}
	};

	private static <T, Context extends RuntimeContext> Expression<Context, T> pureExpression() {
		return new Expression<Context, T>() {
			@Override
			public boolean dependsOnInput() {
				return false;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, T in, Path<T> ipath, Output<T> output) {
			}
		};
	}

	@Test
	public void exposesFunctions() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("uuid4", 0),
				FunctionSignature.of("uuid3", 1),
				FunctionSignature.of("uuid5", 1));
	}

	@Test
	public void functionContract() {
		ModuleImpl module = new ModuleImpl();
		Function uuid3 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid3", 1)));
		Expression<RuntimeContext, JsonNode> uuid3Expr = uuid3.bind(BIND_CONTEXT, Collections.singletonList(pureExpression()));
		assertThat(uuid3Expr.dependsOnInput()).isTrue();
		assertThat(uuid3Expr.dependsOnExternalState()).isFalse();
		Function uuid5 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid5", 1)));
		Expression<RuntimeContext, JsonNode> uuid5Expr = uuid5.bind(BIND_CONTEXT, Collections.singletonList(pureExpression()));
		assertThat(uuid5Expr.dependsOnInput()).isTrue();
		assertThat(uuid5Expr.dependsOnExternalState()).isFalse();
		Function uuid4 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid4", 0)));
		Expression<RuntimeContext, JsonNode> uuid4Expr = uuid4.bind(BIND_CONTEXT, Collections.emptyList());
		assertThat(uuid4Expr.dependsOnInput()).isFalse();
		assertThat(uuid4Expr.dependsOnExternalState()).isTrue();
	}
}
