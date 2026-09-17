package net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess;

import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class IdentifierFieldAccess<JsonNode> extends AbstractFieldAccess<JsonNode> {
	private final String field;

	@Override
	public Cardinality getCardinality() {
		if (target.getCardinality() == Cardinality.ZERO)
			return Cardinality.ZERO;
		if (!permissive && target.getCardinality() == Cardinality.ONE)
			return Cardinality.ONE;
		return Cardinality.UNKNOWN;
	}

	public IdentifierFieldAccess(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> obj, String field, boolean permissive, Version version, int targetOutputIndex) {
		super(jsonProvider, obj, permissive, version, targetOutputIndex);
		this.field = field;
	}

	@Override
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		Expression<StackFrame, JsonNode> rewritten = rewriter.rewrite(target);
		return rewritten == target ? this : new IdentifierFieldAccess<>(jsonProvider, rewritten, field, permissive, version, targetOutputIndex);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		target.apply(frame, in, path, (pobj, ppath) -> {
			memory.countOutput(targetOutputIndex);
			emitObjectFieldPath(jsonProvider, permissive, field, pobj, ppath, output, !(path instanceof UntrackedPath), version);
		});
	}
}
