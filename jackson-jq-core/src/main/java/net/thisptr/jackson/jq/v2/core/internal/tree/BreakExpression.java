package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.exception.JsonQueryBreakException;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class BreakExpression<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private final String name;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ZERO;
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

	public BreakExpression(String name) {
		this.name = name;
	}

	/**
	 * The label this unwinds to.
	 */
	public String name() {
		return name;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		throw new JsonQueryBreakException(name);
	}
}
