package net.thisptr.jackson.jq.v2.ext.uuid;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleDiscoveryTest {
	@Test
	public void exposesOnlyTheUuidModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/uuid").doesNotContainKey("jackson-jq/extras");

		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		assertThat(BuiltinFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6, scope))
				.doesNotContainKeys("uuid3/1", "uuid4/0", "uuid5/1");
	}
}
