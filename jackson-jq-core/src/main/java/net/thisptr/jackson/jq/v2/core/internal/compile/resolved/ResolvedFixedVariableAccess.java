package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.Type;

/**
 * Reference to an {@code EnvironmentBuilder.defineVariable}/{@code defineConstant}-registered variable --
 * fixed at compile time, so the {@link Supplier} is bound directly into the tree with no {@code StackFrame}
 * slot and no closure capture at all.
 */
public class ResolvedFixedVariableAccess<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private final String name;
	private final Supplier<JsonNode> supplier;
	private final Type type;

	public ResolvedFixedVariableAccess(String name, Supplier<JsonNode> supplier, Type type) {
		this.name = name;
		this.supplier = supplier;
		this.type = type;
	}

	/**
	 * The type the {@code Environment} declared for this variable, or {@link Type#ANY} when it declared none
	 * -- which is also what a {@code $}-style data import, registered on the compilation rather than on the
	 * {@code Environment}, contributes.
	 */
	public Type type() {
		return type;
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
}
