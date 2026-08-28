package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.compile.BoundArgumentInfo;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Like {@link ResolvedLocalFunctionAccess}, but for a local function the compiler has bound to a
 * {@link BoundArgumentInfo} -- a {@code JqFunctionCompiler}-inlined parameter with known compile-time
 * dependency facts. Kept as a separate class (rather than a nullable field on the base class) so the
 * common, unbound case stays exactly as small/inlinable as before. A bound argument's own
 * {@code FunctionDependsOnInfo} never applies once it's constant-inlined, so unlike the base class this
 * one has no use for it.
 */
public class ResolvedLocalFunctionBoundArgumentAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int slot;
	private final List<Expression<StackFrame, JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;
	private final BoundArgumentInfo boundArgumentInfo;

	public ResolvedLocalFunctionBoundArgumentAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int slot, List<Expression<StackFrame, JsonNode>> args, BoundArgumentInfo boundArgumentInfo, boolean inputFixed) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.slot = slot;
		this.args = args;
		this.boundArgumentInfo = boundArgumentInfo;
		boolean ownInput = boundArgumentInfo.dependsOnInput();
		boolean ownExternal = boundArgumentInfo.dependsOnExternalState();
		this.dependsOnInput = (ownInput && !inputFixed) || args.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = ownExternal || args.stream().anyMatch(Expression::dependsOnExternalState);
		// The callee lives in the same frame this call runs in -- no closure hop needed to find it, so its
		// freeLocalSlots (already numbered relative to that shared frame) are directly comparable/unionable.
		this.freeLocalSlots = FreeVariables.unionAll(args);
		this.hasOpaqueVariableReference = boundArgumentInfo.dependsOnVariables() || FreeVariables.anyOpaqueIn(args);
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
	public Cardinality getCardinality() {
		return args.isEmpty() ? boundArgumentInfo.getCardinality() : Cardinality.UNKNOWN;
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

	@Override
	public String toString() {
		return name;
	}
}
