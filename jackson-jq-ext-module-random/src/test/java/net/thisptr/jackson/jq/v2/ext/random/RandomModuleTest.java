package net.thisptr.jackson.jq.v2.ext.random;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.ClassPathFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

public class RandomModuleTest {
	@Test
	public void returnsAValueInTheExpectedRange() throws JsonQueryException {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);
		env.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));

		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/random\" as ext; ext::random");
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.jsonProvider().createNull(), (val, path) -> results.add(val));
		assertThat(results).hasSize(1);
		assertThat(results.get(0).doubleValue()).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
	}

	@Test
	public void isDiscoverableOnlyAsAModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/random");

		assertThat(ClassPathFunctionLoader.getInstance().listFunctionFactories(Versions.JQ_1_6)).doesNotContainKey(FunctionNameAndArity.of("random", 0));
	}
}
