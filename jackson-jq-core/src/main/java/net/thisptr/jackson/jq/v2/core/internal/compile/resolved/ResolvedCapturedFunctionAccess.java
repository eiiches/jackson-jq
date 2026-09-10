package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.FunctionDependsOnInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ResolvedCapturedFunctionAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int closureSlot;
	private final int frameClosureSlot;
	private final List<Expression<StackFrame, JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;

	public ResolvedCapturedFunctionAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int closureSlot, int frameClosureSlot, List<Expression<StackFrame, JsonNode>> args, @Nullable FunctionDependsOnInfo info, boolean inputFixed) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.closureSlot = closureSlot;
		this.frameClosureSlot = frameClosureSlot;
		this.args = args;
		boolean ownInput = info != null ? info.dependsOnInput() : true;
		boolean ownExternal = info != null ? info.dependsOnExternalState() : true;
		this.dependsOnInput = (ownInput && !inputFixed) || args.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = ownExternal || args.stream().anyMatch(Expression::dependsOnExternalState);
		// Finding the callee itself already crosses a closure hop -- stay unconditionally opaque for the
		// "own" contribution (matching ResolvedCapturedVariableAccess's "defs stay conservative"
		// precedent); only args, evaluated in the caller's own frame, are ever subtractable.
		this.freeLocalSlots = FreeVariables.unionAll(args);
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
	}

	public List<Expression<StackFrame, JsonNode>> args() {
		return args;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return true;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Closure closure = (Closure) frame.get(frameClosureSlot);
		Function factory = closure != null ? (Function) closure.get(closureSlot) : null;
		if (factory == null) {
			throw new JsonQueryException("Function " + name + " is not defined");
		}
		factory.bindArguments(jsonProvider, args, version).apply(frame, in, path, output);
	}
}
