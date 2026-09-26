package net.thisptr.jackson.jq.v2.ext.os;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.TypeCheckMode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HostnameModuleTest {
	private static final Jackson2JsonProvider JSON = Jackson2JsonProvider.getInstance();

	@Test
	void registersBothSignaturesAndDeclaresExternalState() {
		ModuleImpl module = new ModuleImpl();
		assertThat(module.getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("hostname", 0), FunctionSignature.of("hostname", 1));
		module.getFunctions().forEach((signature, function) -> {
			int arity = Objects.requireNonNull(signature.arity());
			List<TypeScheme<FunctionType>> schemes = function.types(Versions.JQ_1_7, arity);
			assertThat(schemes).isNotEmpty();
			assertThat(schemes.get(0).body().parameterTypes()).hasSize(arity);
			ExpressionProperties properties = function.analyze(Versions.JQ_1_7, arity == 0
					? List.of() : List.of(new ExpressionProperties(Cardinality.ONE, true, false)));
			assertThat(properties.dependsOnExternalState()).isTrue();
			assertThat(properties.dependsOnInput()).isEqualTo(arity == 1);
		});
	}

	@Test
	void findsTheModuleAndInfersStringResults() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(JSON, Versions.JQ_1_7).build();
		CompileOptions strict = CompileOptions.newBuilder().setTypeCheckMode(TypeCheckMode.STRICT).build();
		assertThat(environment.compile("import \"jackson-jq/os\" as os; os::hostname", strict).getType().outputType())
				.isSameAs(StringType.getInstance());
		assertThat(environment.compile("import \"jackson-jq/os\" as os; os::hostname({fqdn: true})", strict)
				.getType().outputType()).isSameAs(StringType.getInstance());
	}

	@Test
	void evaluatesDefaultAndFqdnOptions() throws JsonQueryException {
		List<Boolean> requested = new ArrayList<>();
		Environment<JsonNode> environment = environment(fqdn -> {
			requested.add(fqdn);
			return new HostnameFunction.HostNames("short", "host.example.test", "192.0.2.1");
		});
		assertThat(run(environment, "os::hostname")).containsExactly("short");
		assertThat(run(environment, "os::hostname({})")).containsExactly("short");
		assertThat(run(environment, "os::hostname({fqdn: false})")).containsExactly("short");
		assertThat(run(environment, "os::hostname({fqdn: true})")).containsExactly("host.example.test");
		assertThat(requested).containsExactly(false, false, false, true);
	}

	@Test
	void rejectsInvalidOptionsAndUnresolvedFqdn() {
		Environment<JsonNode> environment = environment(fqdn ->
				new HostnameFunction.HostNames("short", "192.0.2.1", "192.0.2.1"));
		assertThatThrownBy(() -> run(environment, "os::hostname(null)"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run(environment, "os::hostname({other: true})"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("unknown member: other");
		assertThatThrownBy(() -> run(environment, "os::hostname({fqdn: 1})"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("fqdn must be a boolean");
		assertThatThrownBy(() -> run(environment, "os::hostname({fqdn: true})"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("fully qualified domain name");
		Environment<JsonNode> otherAddress = environment(fqdn ->
				new HostnameFunction.HostNames("short", "203.0.113.9", "192.0.2.1"));
		assertThatThrownBy(() -> run(otherAddress, "os::hostname({fqdn: true})"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("fully qualified domain name");
	}

	@Test
	void reportsHostLookupFailure() {
		Environment<JsonNode> environment = environment(fqdn -> {
			throw new UnknownHostException("lookup failed");
		});
		assertThatThrownBy(() -> run(environment, "os::hostname"))
				.isInstanceOf(JsonQueryException.class).hasMessageContaining("lookup failed");
	}

	private static Environment<JsonNode> environment(HostnameFunction.HostnameLookup lookup) {
		HostnameFunction function = new HostnameFunction(lookup);
		JavaModule module = () -> Map.of(
				FunctionSignature.of("hostname", 0), function,
				FunctionSignature.of("hostname", 1), function);
		return EnvironmentBuilder.withDefaultLoaders(JSON, Versions.JQ_1_7)
				.importModule(module, "os")
				.build();
	}

	private static List<String> run(Environment<JsonNode> environment, String source) throws JsonQueryException {
		JsonQuery<JsonNode> query = environment.compile(source);
		List<String> result = new ArrayList<>();
		query.apply(JSON.createNull(), node -> result.add(node.asText()));
		return result;
	}
}
