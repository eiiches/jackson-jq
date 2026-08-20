package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CoreJqLibraryTest {
	@Test
	public void discoverableThroughServiceLoader() {
		assertThat(ServiceLoader.load(JqLibrary.class, getClass().getClassLoader()))
				.anyMatch(CoreJqLibrary.class::isInstance);
		assertThat(new CoreJqLibrary().getFunctions()).hasSize(61);
	}

	@Test
	public void appliesVersionRanges() {
		Environment<JsonNode> env15 = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5).build();
		assertThatCode(() -> env15.compile("paths")).doesNotThrowAnyException();
		assertThatCode(() -> env15.compile("first(empty)")).doesNotThrowAnyException();
		assertThatThrownBy(() -> env15.compile("walk(.)")).isInstanceOf(JsonQueryException.class);
		assertThatThrownBy(() -> env15.compile("pick(.)")).isInstanceOf(JsonQueryException.class);

		Environment<JsonNode> env17 = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_7).build();
		assertThatCode(() -> env17.compile("paths")).doesNotThrowAnyException();
		assertThatCode(() -> env17.compile("first(empty)")).doesNotThrowAnyException();
		assertThatCode(() -> env17.compile("walk(.)")).doesNotThrowAnyException();
		assertThatCode(() -> env17.compile("pick(.)")).doesNotThrowAnyException();
	}
}
