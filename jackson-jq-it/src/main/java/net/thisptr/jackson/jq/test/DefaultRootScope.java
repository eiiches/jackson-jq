package net.thisptr.jackson.jq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;

public class DefaultRootScope {
	private static final Map<Version, Scope<JsonNode>> ROOT_SCOPES = new ConcurrentHashMap<>();

	public static Scope<JsonNode> getInstance(final Version version) {
		return ROOT_SCOPES.computeIfAbsent(version, v -> {
			final Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
			BuiltinFunctionLoader.getInstance().loadFunctions(v, scope);
			return scope;
		});
	}
}
