package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Reference to an {@code EnvironmentBuilder.declareVariable}-registered variable -- no compile-time value,
 * so it's read from {@code StackFrame.getEnclosingMemory()}'s flat global-slots array (populated once per
 * top-level {@code apply()} call from {@code JsonQueryBindings}, before the query body runs). The same
 * {@code globalIndex} is valid from any {@code def}-nesting depth, since one {@code StackMemory} backs
 * exactly one top-level {@code apply()} call -- no closure capture needed.
 */
public class ResolvedGlobalVariableAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final int globalIndex;

	public ResolvedGlobalVariableAccess(String name, int globalIndex) {
		this.name = name;
		this.globalIndex = globalIndex;
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

	// No binding-construct node in the AST could ever capture/subtract a global variable -- it's
	// registered on the Environment, entirely outside the compiled expression.
	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return true;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		@Var Supplier<JsonNode> valueSupplier = null;
		Object raw = frame.getEnclosingMemory().getGlobal(globalIndex);
		if (raw instanceof Supplier) {
			@SuppressWarnings("unchecked")
			Supplier<JsonNode> effectiveSupplier = (Supplier<JsonNode>) raw;
			valueSupplier = effectiveSupplier;
		}
		if (valueSupplier == null)
			throw new JsonQueryException(String.format("Variable $%s is not defined", name));
		JsonNode val = valueSupplier.get();
		if (val == null)
			throw new JsonQueryException(String.format("Variable $%s evaluated to null", name));
		output.emit(val, UntrackedPath.getInstance());
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
