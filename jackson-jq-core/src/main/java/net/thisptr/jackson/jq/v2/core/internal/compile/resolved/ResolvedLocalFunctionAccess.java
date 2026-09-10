package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.FunctionDependsOnInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ResolvedLocalFunctionAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int slot;
	private final List<Expression<StackFrame, JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ResolvedLocalFunctionAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int slot, List<Expression<StackFrame, JsonNode>> args, @Nullable FunctionDependsOnInfo info, boolean inputFixed) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.slot = slot;
		this.args = args;
		boolean ownInput = info != null ? info.dependsOnInput() : true;
		boolean ownExternal = info != null ? info.dependsOnExternalState() : true;
		this.dependsOnInput = (ownInput && !inputFixed) || args.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = ownExternal || args.stream().anyMatch(Expression::dependsOnExternalState);
		// The callee lives in the same frame this call runs in -- no closure hop needed to find it, so its
		// freeLocalSlots (already numbered relative to that shared frame) are directly comparable/unionable.
		Set<Integer> free = new HashSet<>(info != null ? info.freeLocalSlots() : Collections.emptySet());
		free.addAll(FreeVariables.unionAll(args));
		this.freeLocalSlots = free;
		this.hasOpaqueVariableReference = info == null || info.hasOpaqueVariableReference() || FreeVariables.anyOpaqueIn(args);
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
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
		return hasOpaqueVariableReference;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Function factory = (Function) frame.get(slot);
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		factory.bindArguments(jsonProvider, args, version).apply(frame, in, ipath, output);
	}
}
