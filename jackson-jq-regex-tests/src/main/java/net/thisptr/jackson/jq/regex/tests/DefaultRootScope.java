package net.thisptr.jackson.jq.regex.tests;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;

public class DefaultRootScope {
	private static final Map<Version, Scope> ROOT_SCOPES = new ConcurrentHashMap<>();

	public static Scope getInstance(final Version version) {
		return ROOT_SCOPES.computeIfAbsent(version, v -> {
			final Scope scope = Scope.newEmptyScope();
			final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			BuiltinFunctionLoader.getInstance().loadFunctions(classLoader, v, scope);
			return scope;
		});
	}
}
