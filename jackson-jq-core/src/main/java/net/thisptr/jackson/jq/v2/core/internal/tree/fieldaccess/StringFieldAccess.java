package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.tree.ThisObject;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class StringFieldAccess<JsonNode> extends FieldAccess<JsonNode> {
	private Expression<StackFrame, JsonNode> field;

	@Override
	public Cardinality getCardinality() {
		return !permissive
				? CardinalityUtils.multiply(target.getCardinality(), field.getCardinality())
				: (target.getCardinality() == Cardinality.ZERO || field.getCardinality() == Cardinality.ZERO ? Cardinality.ZERO : Cardinality.UNKNOWN);
	}

	public StringFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> obj, Expression<StackFrame, JsonNode> field, boolean permissive, Version version) {
		super(jsonProvider, obj, permissive, version);
		this.field = field;
	}

	public Expression<StackFrame, JsonNode> key() {
		return field;
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
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (!(target instanceof ThisObject))
			builder.append(target.toString());
		builder.append(".");
		builder.append(field);
		if (permissive)
			builder.append("?");
		return builder.toString();
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		field.apply(frame, in, UntrackedPath.getInstance(), (key, opath) -> {
			target.apply(frame, in, path, (pobj, ppath) -> {
				if (!jsonProvider.isString(key) && !permissive)
					throw new IllegalStateException(); // FIXME: exception type
				emitObjectFieldPath(jsonProvider, permissive, jsonProvider.getString(key), pobj, ppath, output, !(path instanceof UntrackedPath), version);
			});
		});
	}
}
