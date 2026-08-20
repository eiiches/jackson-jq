package net.thisptr.jackson.jq.v2.ext.uuid;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {

	@Test
	public void exposesFunctions() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("uuid4", 0),
				FunctionSignature.of("uuid3", 1),
				FunctionSignature.of("uuid5", 1));
	}
}
