package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * A bounded, compiler-materialized constant expression.
 */
public final class PrecomputedConstantExpression<JsonNode> implements ConstantExpression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> delegate;
	private final List<JsonNode> results;

	public PrecomputedConstantExpression(JsonProvider<JsonNode> jsonProvider, Expression<StackFrame, JsonNode> delegate, List<JsonNode> results) {
		this.delegate = delegate;
		List<JsonNode> snapshot = new ArrayList<>(results.size());
		for (JsonNode result : results)
			snapshot.add(jsonProvider.deepCopy(result));
		this.results = Collections.unmodifiableList(snapshot);
	}

	@Override
	public List<JsonNode> getConstantResults() {
		return results;
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
	public void apply(StackFrame context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		delegate.apply(context, in, ipath, output);
	}
}
