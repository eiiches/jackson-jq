package net.thisptr.jackson.jq.v2.core.internal.compile;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;

/**
 * Where the source being compiled sits in the module graph: which module it is (if any), and the
 * {@link ModuleResolver} that turns its import statements into modules. The compiler threads one of
 * these the way it used to thread a bare "current module".
 */
public final class ModuleScope<JsonNode> {
	private final ModuleResolver<JsonNode> resolver;
	private final @Nullable JqModule<JsonNode> currentModule;

	ModuleScope(ModuleResolver<JsonNode> resolver, @Nullable JqModule<JsonNode> currentModule) {
		this.resolver = resolver;
		this.currentModule = currentModule;
	}

	/**
	 * A scope for a whole query: not inside any module, with a resolver of its own. One per
	 * compilation, which is what scopes everything that resolver remembers.
	 */
	public static <JsonNode> ModuleScope<JsonNode> root(Environment<JsonNode> env) {
		return new ModuleScope<>(new ModuleResolver<>(env), null);
	}

	public JavaModule resolveModule(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		return resolver.resolveModule(currentModule, path, metadata);
	}

	public JsonNode resolveData(String path, Maybe<JsonNode> metadata) throws JsonQueryException {
		return resolver.resolveData(currentModule, path, metadata);
	}

	/**
	 * Makes a module an {@code Environment} was handed usable -- compiling it if it is jq source.
	 */
	public JavaModule materialize(Module module) throws JsonQueryException {
		return resolver.materialize(module);
	}
}
