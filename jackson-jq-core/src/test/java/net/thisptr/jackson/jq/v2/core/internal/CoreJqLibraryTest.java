package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;

import static org.assertj.core.api.Assertions.assertThat;

public class CoreJqLibraryTest {
	@Test
	public void discoverableThroughServiceLoader() {
		assertThat(ServiceLoader.load(JqLibrary.class, getClass().getClassLoader()))
				.anyMatch(CoreJqLibrary.class::isInstance);
		assertThat(new CoreJqLibrary().getFunctions()).hasSize(61);
	}

	@Test
	public void appliesVersionRanges() {
		net.thisptr.jackson.jq.v2.core.Environment<JsonNode> env15 = new net.thisptr.jackson.jq.v2.core.Environment<>(net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		assertThat(env15.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("paths", 0))).isNotNull();
		assertThat(env15.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("first", 1))).isNotNull();
		assertThat(env15.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("walk", 1))).isNull();
		assertThat(env15.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("pick", 1))).isNull();

		net.thisptr.jackson.jq.v2.core.Environment<JsonNode> env17 = new net.thisptr.jackson.jq.v2.core.Environment<>(net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_7);
		assertThat(env17.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("paths", 0))).isNotNull();
		assertThat(env17.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("first", 1))).isNotNull();
		assertThat(env17.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("walk", 1))).isNotNull();
		assertThat(env17.getFunctionFactory(net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity.of("pick", 1))).isNotNull();
	}
}
