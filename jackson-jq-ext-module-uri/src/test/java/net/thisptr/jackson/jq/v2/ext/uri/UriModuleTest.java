package net.thisptr.jackson.jq.v2.ext.uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class UriModuleTest {
	private static final BindContext<JsonNode> BIND_CONTEXT = new BindContext<>() {
		@Override
		public JsonProvider<JsonNode> getJsonProvider() {
			return Jackson2JsonProvider.getInstance();
		}

		@Override
		public Version getJqVersion() {
			return Versions.JQ_1_6;
		}
	};

	@Test
	public void parsesAndDecodesUris() throws JsonQueryException {
		List<JsonNode> parsed = run("\"http://user@example.com:8080/path?foo=a%20b#fragment\" | ext::uriparse | [.scheme, .host, .port, .query_obj.foo, .fragment]");
		assertThat(parsed).hasSize(1);
		assertThat(parsed.get(0).toString()).isEqualTo("[\"http\",\"example.com\",8080,\"a b\",\"fragment\"]");
		assertThat(run("\"%66%6f%6f\" | ext::uridecode")).extracting(JsonNode::textValue).containsExactly("foo");
	}

	@Test
	public void exposesFunctions() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("uridecode", 0),
				FunctionSignature.of("uriparse", 0));
	}

	@Test
	public void functionContract() {
		ModuleImpl module = new ModuleImpl();
		module.getFunctions().values().forEach(fn -> {
			Expression<RuntimeContext, JsonNode> expr = fn.bind(BIND_CONTEXT, Collections.emptyList());
			assertThat(expr.dependsOnInput()).isTrue();
			assertThat(expr.dependsOnExternalState()).isFalse();
		});
	}

	private List<JsonNode> run(String expression) throws JsonQueryException {
		Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(Jackson2JsonProvider.getInstance(), Versions.JQ_1_6)
				.build();
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/uri\" as ext; " + expression);
		List<JsonNode> results = new ArrayList<>();
		query.apply(env.getJsonProvider().createNull(), results::add);
		return results;
	}
}
