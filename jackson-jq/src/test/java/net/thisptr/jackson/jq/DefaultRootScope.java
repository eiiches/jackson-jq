package net.thisptr.jackson.jq;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultRootScope {
	private static final Map<Version, Scope> ROOT_SCOPES = new ConcurrentHashMap<>();

	public static Scope getInstance(final Version version) {
		return ROOT_SCOPES.computeIfAbsent(version, v -> {
			final Scope scope = Scope.newEmptyScope();
			scope.loadFunctions(Scope.class.getClassLoader(), v);
			return scope;
		});
	}
}
