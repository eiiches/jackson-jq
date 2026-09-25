package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;

public class ThisObject<JsonNode> implements AnalyzedExpression<JsonNode>, FreeVariables {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final List<TypeScheme<FilterType>> TYPE_SCHEMES = List.of(
			TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FilterType.of(INPUT, INPUT)));

	@Override
	public List<TypeScheme<FilterType>> getTypeSchemes() {
		return TYPE_SCHEMES;
	}

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ONE;
	}

	@Override
	public boolean dependsOnInput() {
		return true;
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
		output.emit(in, ipath);
	}
}
