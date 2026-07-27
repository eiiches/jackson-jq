package net.thisptr.jackson.jq.regex.joni;

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
			BuiltinFunctionLoader.getInstance().loadFunctions(v, scope);
			return scope;
		});
	}
}
