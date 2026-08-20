package net.thisptr.jackson.jq.v2.ext.uuid;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {

	@Test
	public void exposesDefinitionsViaModuleMeta() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getModuleMeta().getDefinitions()).containsExactlyInAnyOrder(
				FunctionSignature.of("uuid4", 0),
				FunctionSignature.of("uuid3", 1),
				FunctionSignature.of("uuid5", 1));
	}
}
