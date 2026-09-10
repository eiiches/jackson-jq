package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class TryCatch<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<StackFrame, JsonNode> tryExpr;
	private final @Nullable Expression<StackFrame, JsonNode> catchExpr;

	@Override
	public Cardinality getCardinality() {
		if (tryExpr.getCardinality() == Cardinality.ZERO && (catchExpr == null || catchExpr.getCardinality() == Cardinality.ZERO))
			return Cardinality.ZERO;
		return Cardinality.UNKNOWN;
	}

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> tryExpr, @Nullable Expression<StackFrame, JsonNode> catchExpr) {
		this.jsonProvider = jsonProvider;
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
	}

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> tryExpr) {
		this(jsonProvider, tryExpr, null);
	}

	// catchExpr is already compiled under the correct shielded context (see Compiler's
	// TryCatchAstNode handling), so this is just a flat OR, same as everywhere else.
	@Override
	public boolean dependsOnInput() {
		return tryExpr.dependsOnInput() || (catchExpr != null && catchExpr.dependsOnInput());
	}

	@Override
	public boolean dependsOnExternalState() {
		return tryExpr.dependsOnExternalState() || (catchExpr != null && catchExpr.dependsOnExternalState());
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(tryExpr, catchExpr);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(tryExpr, catchExpr);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		try {
			tryExpr.apply(frame, in, path, output);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(frame, e.toJson(jsonProvider), path instanceof UntrackedPath ? UntrackedPath.getInstance() : UnrepresentablePath.getInstance(), output);
			}
		}
	}
}
