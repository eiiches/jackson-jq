package net.thisptr.jackson.jq.v2.ext.uri;

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

		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		assertThat(BuiltinFunctionLoader.getInstance().listFunctions(Versions.JQ_1_6, scope)).doesNotContainKeys("uriparse/0", "uridecode/0");
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		scope.setModuleLoader(new ClassPathModuleLoader<>(getClass().getClassLoader()));
		JsonQuery<JsonNode> query = JsonQuery.compile("import \"jackson-jq/uri\" as ext; " + expression, Versions.JQ_1_6);
		List<JsonNode> results = new ArrayList<>();
		query.apply(scope, scope.jsonProvider().createNull(), results::add);
		return results;
	}
}
