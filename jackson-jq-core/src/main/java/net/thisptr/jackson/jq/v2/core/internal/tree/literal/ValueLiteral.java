package net.thisptr.jackson.jq.v2.core.internal.tree.literal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;

public class ValueLiteral<JsonNode> implements ConstantExpression<StackFrame, JsonNode>, AnalyzedExpression<JsonNode>, FreeVariables {
	private final JsonNode value;
	private final Type type;
	// The literal as it was written, for analyses that have no JsonProvider to read the node with. Null
	// whenever the literal is of another kind, or was not built from source.
	private final @Nullable String stringLiteral;
	private final @Nullable BigDecimal numberLiteral;

	public ValueLiteral(JsonNode value) {
		this(AnyType.getInstance(), value, null, null);
	}

	public ValueLiteral(Type type, JsonNode value) {
		this(type, value, null, null);
	}

	public ValueLiteral(Type type, JsonNode value, @Nullable String stringLiteral) {
		this(type, value, stringLiteral, null);
	}

	public ValueLiteral(Type type, JsonNode value, BigDecimal numberLiteral) {
		this(type, value, null, numberLiteral);
	}

	private ValueLiteral(Type type, JsonNode value, @Nullable String stringLiteral,
						 @Nullable BigDecimal numberLiteral) {
		this.type = type;
		this.value = value;
		this.stringLiteral = stringLiteral;
		this.numberLiteral = numberLiteral;
	}

	public JsonNode value() {
		return value;
	}

	public @Nullable String stringLiteral() {
		return stringLiteral;
	}

	public @Nullable BigDecimal numberLiteral() {
		return numberLiteral;
	}

	@Override
	public List<TypeScheme<FilterType>> getTypeSchemes() {
		return List.of(TypeScheme.of(FilterType.of(AnyType.getInstance(), type)));
	}

	@Override
	public List<JsonNode> getConstantResults() {
		return Collections.singletonList(value);
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
		output.emit(value, UntrackedPath.getInstance());
	}
}
