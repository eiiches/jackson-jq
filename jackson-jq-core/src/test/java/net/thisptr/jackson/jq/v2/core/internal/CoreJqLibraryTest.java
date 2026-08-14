package net.thisptr.jackson.jq.v2.core.internal;

import java.util.ServiceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.JqLibrary;
import net.thisptr.jackson.jq.v2.spi.Scope;

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
		Scope<JsonNode> scope15 = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(getClass().getClassLoader(), Versions.JQ_1_5, scope15);
		assertThat(scope15.getFunction("paths", 0)).isNotNull();
		assertThat(scope15.getFunction("first", 1)).isNotNull();
		assertThat(scope15.getFunction("walk", 1)).isNull();
		assertThat(scope15.getFunction("pick", 1)).isNull();

		Scope<JsonNode> scope17 = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		BuiltinFunctionLoader.getInstance().loadFunctions(getClass().getClassLoader(), Versions.JQ_1_7, scope17);
		assertThat(scope17.getFunction("paths", 0)).isNotNull();
		assertThat(scope17.getFunction("first", 1)).isNotNull();
		assertThat(scope17.getFunction("walk", 1)).isNotNull();
		assertThat(scope17.getFunction("pick", 1)).isNotNull();
	}
}
