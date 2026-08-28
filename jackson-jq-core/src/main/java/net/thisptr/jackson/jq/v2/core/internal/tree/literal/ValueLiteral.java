package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.FreeVariables;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class ValueLiteral<JsonNode> implements ConstantExpression<StackFrame, JsonNode>, FreeVariables {
	protected final JsonProvider<JsonNode> jsonProvider;

	protected ValueLiteral(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public abstract JsonNode value();

	@Override
	public List<JsonNode> getConstantResults() {
		return Collections.singletonList(value());
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
		output.emit(value(), UntrackedPath.getInstance());
	}
}
