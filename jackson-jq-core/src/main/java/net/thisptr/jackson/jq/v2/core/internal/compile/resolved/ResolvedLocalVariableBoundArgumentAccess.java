package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.BoundArgumentInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Like {@link ResolvedLocalVariableAccess}, but for a local variable the compiler has bound to a
 * {@link BoundArgumentInfo} -- a {@code JqFunctionCompiler}-inlined parameter with known compile-time
 * dependency facts. Kept as a separate class (rather than a nullable field on the base class) so the
 * common, unbound case stays exactly as small/inlinable as before.
 */
public class ResolvedLocalVariableBoundArgumentAccess<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final String name;
	private final int slot;
	private final BoundArgumentInfo boundArgumentInfo;

	public ResolvedLocalVariableBoundArgumentAccess(String name, int slot, BoundArgumentInfo boundArgumentInfo) {
		this.name = name;
		this.slot = slot;
		this.boundArgumentInfo = boundArgumentInfo;
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	@Override
	public boolean dependsOnInput() {
		return boundArgumentInfo.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return boundArgumentInfo.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.singleton(slot);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return boundArgumentInfo.dependsOnVariables();
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		PathAndValue<JsonNode> val = StackFrameValues.asPathAndValue(frame.get(slot));
		if (val != null && val.getValue() != null) {
			output.emit(val.getValue(), path instanceof UntrackedPath ? UntrackedPath.getInstance() : val.getPath());
			return;
		}
		throw new JsonQueryException(String.format("$%s is not defined", name));
	}
}
