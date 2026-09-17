package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class StringFieldAccess<JsonNode> extends AbstractFieldAccess<JsonNode> {
	private Expression<StackFrame, JsonNode> field;
	private final int fieldOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return !permissive
				? CardinalityUtils.multiply(target.getCardinality(), field.getCardinality())
				: (target.getCardinality() == Cardinality.ZERO || field.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN);
	}

	public StringFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> obj, Expression<StackFrame, JsonNode> field, boolean permissive, Version version, int targetOutputIndex, int fieldOutputIndex) {
		super(jsonProvider, obj, permissive, version, targetOutputIndex);
		this.fieldOutputIndex = fieldOutputIndex;
		this.field = field;
	}

	@Override
	public boolean dependsOnInput() {
		return super.dependsOnInput() || field.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return super.dependsOnExternalState() || field.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(target, field);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(target, field);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		field.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			memory.countOutput(fieldOutputIndex);
			target.apply(frame, in, path, (pobj, ppath) -> {
				memory.countOutput(targetOutputIndex);
				if (!jsonProvider.isString(key) && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.getString(key), pobj, ppath, output, !(path instanceof UntrackedPath), version);
			});
		});
	}
}
