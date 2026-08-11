package net.thisptr.jackson.jq.v2.ext.time;

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

public class TimeModuleTest {
	@Test
	public void formatsAndParsesTime() throws JsonQueryException {
		assertThat(run("1477162342372 | ext::strftime(\"yyyy-MM-dd HH:mm:ss.SSSXXX\"; \"UTC\")"))
				.extracting(JsonNode::textValue)
				.containsExactly("2016-10-22 18:52:22.372Z");
		assertThat(run("\"2016-10-22 18:52:22.372\" | ext::strptime(\"yyyy-MM-dd HH:mm:ss.SSS\"; \"UTC\")"))
				.extracting(JsonNode::longValue)
				.containsExactly(1477162342372L);
		assertThat(run("1477162342372 | ext::strftime(\"yyyy-MM-dd HH:mm:ss.SSS\") | ext::strptime(\"yyyy-MM-dd HH:mm:ss.SSS\")"))
				.extracting(JsonNode::longValue)
				.containsExactly(1477162342372L);
	}

	@Test
	public void returnsTheCurrentTimestamp() throws JsonQueryException {
		long before = System.currentTimeMillis();
		List<JsonNode> result = run("ext::timestamp");
		long after = System.currentTimeMillis();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).longValue()).isBetween(before, after);
	}

	@Test
	public void isDiscoverableOnlyAsAModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/time");

		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		assertThat(BuiltinFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6, scope))
				.doesNotContainKeys("timestamp/0", "strftime/1", "strftime/2", "strptime/1", "strptime/2");
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		scope.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));
		JsonQuery<JsonNode> query = JsonQuery.compile("import \"jackson-jq/time\" as ext; " + expression, Versions.JQ_1_6);
		List<JsonNode> results = new ArrayList<>();
		query.apply(scope, scope.jsonProvider().createNull(), results::add);
		return results;
	}
}
