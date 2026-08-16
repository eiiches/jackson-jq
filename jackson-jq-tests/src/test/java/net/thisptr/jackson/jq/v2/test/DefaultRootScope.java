package net.thisptr.jackson.jq.v2.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;

public class DefaultRootScope {
	private static final Map<Version, Scope<JsonNode>> ROOT_SCOPES = new ConcurrentHashMap<>();

	public static Scope<JsonNode> getInstance(Version version) {
		return ROOT_SCOPES.computeIfAbsent(version, v -> {
			return Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		});
	}
}
