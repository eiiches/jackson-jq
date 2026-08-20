package net.thisptr.jackson.jq.v2.ext.uuid;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {
	@Test
	public void exposesOnlyTheUuidModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/uuid").doesNotContainKey("jackson-jq/extras");

		assertThat(ClassPathFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6))
				.doesNotContainKeys(FunctionSignature.of("uuid3", 1), FunctionSignature.of("uuid4", 0), FunctionSignature.of("uuid5", 1));
	}
}
