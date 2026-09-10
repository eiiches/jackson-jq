package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.Collections;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.path.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {
	private static <T, Context> Expression<Context, T> pureExpression() {
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
		Expression<Object, JsonNode> uuid3Expr = uuid3.bindArguments(Jackson2JsonProviderImpl.getInstance(), Collections.singletonList(pureExpression()), Versions.JQ_1_6);
		assertThat(uuid3Expr.dependsOnInput()).isTrue();
		assertThat(uuid3Expr.dependsOnExternalState()).isFalse();
		Function uuid5 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid5", 1)));
		Expression<Object, JsonNode> uuid5Expr = uuid5.bindArguments(Jackson2JsonProviderImpl.getInstance(), Collections.singletonList(pureExpression()), Versions.JQ_1_6);
		assertThat(uuid5Expr.dependsOnInput()).isTrue();
		assertThat(uuid5Expr.dependsOnExternalState()).isFalse();
		Function uuid4 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid4", 0)));
		Expression<Object, JsonNode> uuid4Expr = uuid4.bindArguments(Jackson2JsonProviderImpl.getInstance(), Collections.emptyList(), Versions.JQ_1_6);
		assertThat(uuid4Expr.dependsOnInput()).isFalse();
		assertThat(uuid4Expr.dependsOnExternalState()).isTrue();
	}
}
