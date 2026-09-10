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
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class TryCatch<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private static final Version DOWNSTREAM_ERRORS_ESCAPE_SINCE = Version.of(1, 7);

	private static final class DownstreamException extends JsonQueryException {
		private static final long serialVersionUID = 1L;

		private final transient Object boundary;
		private final JsonQueryException exception;

		private DownstreamException(Object boundary, JsonQueryException exception) {
			super(exception);
			this.boundary = boundary;
			this.exception = exception;
		}
	}

	private final JsonProvider<JsonNode> jsonProvider;
	private final Expression<StackFrame, JsonNode> tryExpr;
	private final @Nullable Expression<StackFrame, JsonNode> catchExpr;
	private final Version version;

	@Override
	public Cardinality getCardinality() {
		if (tryExpr.getCardinality() == Cardinality.ZERO && (catchExpr == null || catchExpr.getCardinality() == Cardinality.ZERO))
			return Cardinality.ZERO;
		return Cardinality.UNKNOWN;
	}

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> tryExpr, @Nullable Expression<StackFrame, JsonNode> catchExpr, Version version) {
		this.jsonProvider = jsonProvider;
		this.tryExpr = tryExpr;
		this.catchExpr = catchExpr;
		this.version = version;
	}

	public TryCatch(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> tryExpr, Version version) {
		this(jsonProvider, tryExpr, null, version);
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
		if (version.compareTo(DOWNSTREAM_ERRORS_ESCAPE_SINCE) < 0) {
			applyLegacy(frame, in, path, output);
			return;
		}

		// Output callbacks execute within tryExpr.apply. Tunnel their errors through nested try
		// expressions until the callback's own boundary can restore the original exception.
		Object boundary = new Object();
		try {
			tryExpr.apply(frame, in, path, (value, outputPath) -> {
				try {
					output.emit(value, outputPath);
				} catch (JsonQueryException e) {
					throw new DownstreamException(boundary, e);
				}
			});
		} catch (DownstreamException e) {
			if (e.boundary == boundary)
				throw e.exception;
			throw e;
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(frame, e.toJson(jsonProvider), path instanceof UntrackedPath ? UntrackedPath.getInstance() : UnrepresentablePath.getInstance(), output);
			}
		}
	}

	private void applyLegacy(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		try {
			tryExpr.apply(frame, in, path, output);
		} catch (JsonQueryException e) {
			if (catchExpr != null) {
				catchExpr.apply(frame, e.toJson(jsonProvider), path instanceof UntrackedPath ? UntrackedPath.getInstance() : UnrepresentablePath.getInstance(), output);
			}
		}
	}
}
