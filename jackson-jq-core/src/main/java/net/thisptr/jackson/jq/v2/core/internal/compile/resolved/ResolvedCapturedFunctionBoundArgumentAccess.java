package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.BoundArgumentInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Like {@link ResolvedCapturedFunctionAccess}, but for a captured function the compiler has bound to a
 * {@link BoundArgumentInfo} -- a {@code JqFunctionCompiler}-inlined parameter with known compile-time
 * dependency facts. Kept as a separate class (rather than a nullable field on the base class) so the
 * common, unbound case stays exactly as small/inlinable as before. A bound argument's own
 * {@code FunctionDependsOnInfo} never applies once it's constant-inlined, so unlike the base class this
 * one has no use for it.
 */
public class ResolvedCapturedFunctionBoundArgumentAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int closureSlot;
	private final int frameClosureSlot;
	private final List<Expression<StackFrame, JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final BoundArgumentInfo boundArgumentInfo;

	public ResolvedCapturedFunctionBoundArgumentAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int closureSlot, int frameClosureSlot, List<Expression<StackFrame, JsonNode>> args, BoundArgumentInfo boundArgumentInfo, boolean inputFixed) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.closureSlot = closureSlot;
		this.frameClosureSlot = frameClosureSlot;
		this.args = args;
		this.boundArgumentInfo = boundArgumentInfo;
		boolean ownInput = boundArgumentInfo.dependsOnInput();
		boolean ownExternal = boundArgumentInfo.dependsOnExternalState();
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
		return boundArgumentInfo.dependsOnVariables() || FreeVariables.anyOpaqueIn(args);
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

	@Override
	public String toString() {
		return name;
	}
}
