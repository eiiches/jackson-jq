package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;

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
 * Reference to an {@code EnvironmentBuilder.declareVariable}-registered variable -- no compile-time value,
 * so it's read from {@code StackFrame.getEnclosingMemory()}'s flat global-slots array (prepared when the
 * immutable query view is built and shared read-only by its invocations). The same {@code globalIndex} is
 * valid from any {@code def}-nesting depth, since one {@code StackMemory} backs exactly one top-level
 * {@code apply()} call -- no closure capture needed.
 */
public class ResolvedGlobalVariableAccess<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private final String name;
	private final int globalIndex;
	private final Type type;

	public ResolvedGlobalVariableAccess(String name, int globalIndex, Type type) {
		this.name = name;
		this.globalIndex = globalIndex;
		this.type = type;
	}

	/**
	 * The type the {@code Environment} declared for this variable, or {@link Type#ANY} when it declared none.
	 * Nothing checks a runtime binding against it -- it is what type checking is told, not a constraint.
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
		if (raw instanceof Supplier<?> supplier) {
			@SuppressWarnings("unchecked")
			Supplier<JsonNode> effectiveSupplier = (Supplier<JsonNode>) supplier;
			valueSupplier = effectiveSupplier;
		}
		if (valueSupplier == null)
			throw new JsonQueryException(String.format("Variable $%s is not defined", name));
		JsonNode val = valueSupplier.get();
		if (val == null)
			throw new JsonQueryException(String.format("Variable $%s evaluated to null", name));
		output.emit(val, UntrackedPath.getInstance());
	}
}
