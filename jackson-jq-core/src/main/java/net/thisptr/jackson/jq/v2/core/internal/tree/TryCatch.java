package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.path.UnrepresentablePath;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class TryCatch<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	protected Expression<StackFrame, JsonNode> tryExpr;
	protected @Nullable Expression<StackFrame, JsonNode> catchExpr;

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

	public Expression<StackFrame, JsonNode> tryExpr() {
		return tryExpr;
	}

	public @Nullable Expression<StackFrame, JsonNode> catchExpr() {
		return catchExpr;
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
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		try {
			tryExpr.apply(frame, in, path, output);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(frame, e.getMessageAsJsonNode(jsonProvider), path != null ? UnrepresentablePath.getInstance() : null, output);
			}
		}
	}

	public static class Question<JsonNode> extends TryCatch<JsonNode> {
		public Question(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> tryExpr) {
			super(jsonProvider, tryExpr);
		}

		@Override
		public String toString() {
			return String.format("(%s)?", tryExpr);
		}
	}

	@Override
	public String toString() {
		if (catchExpr != null) {
			return String.format("(try (%s) catch (%s))", tryExpr, catchExpr);
		} else {
			return String.format("(try (%s))", tryExpr);
		}
	}
}
