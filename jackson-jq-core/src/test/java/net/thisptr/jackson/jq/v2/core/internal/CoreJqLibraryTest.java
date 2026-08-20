package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
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
		Environment<JsonNode> env15 = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5);
		assertThat(env15.resolveFunction("paths", 0)).isNotNull();
		assertThat(env15.resolveFunction("first", 1)).isNotNull();
		assertThat(env15.resolveFunction("walk", 1)).isNull();
		assertThat(env15.resolveFunction("pick", 1)).isNull();

		Environment<JsonNode> env17 = new Environment<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_7);
		assertThat(env17.resolveFunction("paths", 0)).isNotNull();
		assertThat(env17.resolveFunction("first", 1)).isNotNull();
		assertThat(env17.resolveFunction("walk", 1)).isNotNull();
		assertThat(env17.resolveFunction("pick", 1)).isNotNull();
	}
}
