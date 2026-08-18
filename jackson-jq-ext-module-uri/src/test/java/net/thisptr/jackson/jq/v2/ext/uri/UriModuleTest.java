package net.thisptr.jackson.jq.v2.ext.uri;

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

public class UriModuleTest {
	@Test
	public void parsesAndDecodesUris() throws JsonQueryException {
		List<JsonNode> parsed = run("\"http://user@example.com:8080/path?foo=a%20b#fragment\" | ext::uriparse | [.scheme, .host, .port, .query_obj.foo, .fragment]");
		assertThat(parsed).hasSize(1);
		assertThat(parsed.get(0).toString()).isEqualTo("[\"http\",\"example.com\",8080,\"a b\",\"fragment\"]");
		assertThat(run("\"%66%6f%6f\" | ext::uridecode")).extracting(JsonNode::textValue).containsExactly("foo");
	}

	@Test
	public void isDiscoverableOnlyAsAModule() {
		ClassPathModuleLoader<JsonNode> modules = new ClassPathModuleLoader<>(getClass().getClassLoader());
		assertThat(modules.loadAllModules()).containsKey("jackson-jq/uri");

		assertThat(ClassPathFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6)).doesNotContainKeys(FunctionNameAndArity.of("uriparse", 0), FunctionNameAndArity.of("uridecode", 0));
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_6);
		env.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/uri\" as ext; " + expression);
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.jsonProvider().createNull(), (val, path) -> results.add(val));
		return results;
	}
}
