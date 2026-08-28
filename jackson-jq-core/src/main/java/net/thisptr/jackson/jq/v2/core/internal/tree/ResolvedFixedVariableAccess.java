package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Reference to an {@code EnvironmentBuilder.defineVariable}/{@code defineConstant}-registered variable --
 * fixed at compile time, so the {@link Supplier} is bound directly into the tree with no {@code StackFrame}
 * slot and no closure capture at all.
 */
public class ResolvedFixedVariableAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final Supplier<JsonNode> supplier;

	public ResolvedFixedVariableAccess(String name, Supplier<JsonNode> supplier) {
		this.name = name;
		this.supplier = supplier;
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	@Override
	public boolean dependsOnInput() {
		return false;
	}

	@Override
	public boolean dependsOnExternalState() {
		return false;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	// The supplier can return a different value on every read (e.g. a counter), so this must stay opaque
	// even though it no longer touches a StackFrame/Closure slot at all -- see
	// EnvironmentPocTest#testDependsOnVariablesClosesOverLocalBindings.
	@Override
	public boolean hasOpaqueVariableReference() {
		return true;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		JsonNode val = supplier.get();
		if (val == null)
			throw new JsonQueryException(String.format("Variable $%s evaluated to null", name));
		output.emit(val, UntrackedPath.getInstance());
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
