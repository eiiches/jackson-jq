package net.thisptr.jackson.jq.v2.ext.random;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

public class RandomModuleTest {
	@Test
	public void returnsAValueInTheExpectedRange() throws JsonQueryException {
		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		scope.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));
		JsonQuery<JsonNode> query = JsonQuery.compile("import \"jackson-jq/random\" as ext; ext::random", Versions.JQ_1_6);
		List<JsonNode> results = new ArrayList<>();
		query.apply(scope, scope.jsonProvider().createNull(), results::add);
		assertThat(results).hasSize(1);
		assertThat(results.get(0).doubleValue()).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
	}

	@Test
	public void isDiscoverableOnlyAsAModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/random");

		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		assertThat(BuiltinFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6, scope)).doesNotContainKey("random/0");
	}
}
