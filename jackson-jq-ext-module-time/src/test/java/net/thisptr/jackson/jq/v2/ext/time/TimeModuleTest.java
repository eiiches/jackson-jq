package net.thisptr.jackson.jq.v2.ext.time;

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

		assertThat(ClassPathFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6))
				.doesNotContainKeys(FunctionNameAndArity.of("timestamp", 0), FunctionNameAndArity.of("strftime", 1), FunctionNameAndArity.of("strftime", 2), FunctionNameAndArity.of("strptime", 1), FunctionNameAndArity.of("strptime", 2));
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);
		env.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/time\" as ext; " + expression);
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.jsonProvider().createNull(), (val, path) -> results.add(val));
		return results;
	}
}
