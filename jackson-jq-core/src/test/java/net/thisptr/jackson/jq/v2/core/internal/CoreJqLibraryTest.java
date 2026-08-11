package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.internal.InternalJqLibrary;

import static org.assertj.core.api.Assertions.assertThat;

public class CoreJqLibraryTest {
	@Test
	public void discoverableThroughServiceLoader() {
		assertThat(ServiceLoader.load(InternalJqLibrary.class, getClass().getClassLoader()))
				.anyMatch(CoreJqLibrary.class::isInstance);
		assertThat(new CoreJqLibrary().getFunctions()).hasSize(61);
	}

	@Test
	public void appliesVersionRanges() {
		final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());

		assertThat(BuiltinFunctionLoader.getInstance().loadFunctionsFromJqLibrary(getClass().getClassLoader(), Versions.JQ_1_5, scope))
				.containsKeys("paths/0", "first/1")
				.doesNotContainKeys("walk/1", "pick/1");
		assertThat(BuiltinFunctionLoader.getInstance().loadFunctionsFromJqLibrary(getClass().getClassLoader(), Versions.JQ_1_7, scope))
				.containsKeys("paths/0", "first/1", "walk/1", "pick/1");
	}
}
