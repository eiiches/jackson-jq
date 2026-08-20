package net.thisptr.jackson.jq.v2.ext.debug;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

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
	public void exposesDefinitionsViaModuleMeta() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getModuleMeta().getDefinitions()).containsExactlyInAnyOrder(
				FunctionSignature.of("debug_scope", 0));
	}
}
