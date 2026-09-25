package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Replays the values and terminal jq error produced by a constant expression.
 */
public final class FoldedErrorExpression<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private final AnalyzedExpression<JsonNode> delegate;
	private final List<JsonNode> values;
	private final JsonQueryException error;

	public FoldedErrorExpression(AnalyzedExpression<JsonNode> delegate, List<JsonNode> values, JsonQueryException error) {
		this.delegate = delegate;
		this.values = Collections.unmodifiableList(values);
		this.error = error;
	}

	@Override
	public Cardinality getCardinality() {
		return delegate.getCardinality();
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

	@Override
	public boolean hasOpaqueVariableReference() {
		return false;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		if (!(ipath instanceof UntrackedPath)) {
			delegate.apply(frame, in, ipath, output);
			return;
		}
		for (JsonNode value : values)
			output.emit(value, UntrackedPath.getInstance());
		throw error;
	}
}
