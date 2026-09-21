package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {
	private static final ExpressionProperties PURE_ARGUMENT = new ExpressionProperties(Cardinality.ONE, false, false);

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
		ExpressionProperties uuid3Properties = uuid3.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT));
		assertThat(uuid3Properties.dependsOnInput()).isTrue();
		assertThat(uuid3Properties.dependsOnExternalState()).isFalse();
		Function uuid5 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid5", 1)));
		ExpressionProperties uuid5Properties = uuid5.analyze(Versions.JQ_1_6, List.of(PURE_ARGUMENT));
		assertThat(uuid5Properties.dependsOnInput()).isTrue();
		assertThat(uuid5Properties.dependsOnExternalState()).isFalse();
		Function uuid4 = Objects.requireNonNull(module.getFunctions().get(FunctionSignature.of("uuid4", 0)));
		ExpressionProperties uuid4Properties = uuid4.analyze(Versions.JQ_1_6, List.of());
		assertThat(uuid4Properties.dependsOnInput()).isFalse();
		assertThat(uuid4Properties.dependsOnExternalState()).isTrue();
	}
}
