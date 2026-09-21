package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.path.utils.PathOperations;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class IdentifierKeyFieldConstruction<JsonNode> implements FieldConstruction<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	public final String key;
	public final @Nullable AnalyzedExpression<JsonNode> value;
	private final Version version;
	private final int valueOutputIndex;

	@Override
	public Cardinality getCardinality() {
		return value == null ? Cardinality.ONE : value.getCardinality();
	}

	public IdentifierKeyFieldConstruction(JsonProvider<JsonNode> jsonProvider, String key, @Nullable AnalyzedExpression<JsonNode> value, Version version, int valueOutputIndex) {
		this.jsonProvider = jsonProvider;
		this.key = key;
		this.value = value;
		this.version = version;
		this.valueOutputIndex = valueOutputIndex;
	}

	// `{foo}` shorthand implicitly reads `in` when value is absent.
	@Override
	public boolean dependsOnInput() {
		return value == null || value.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return value != null && value.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.union(value);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.anyOpaque(value);
	}

	@Override
	public FieldConstruction<JsonNode> rewriteExpressions(ExpressionRewriter<JsonNode> rewriter) {
		if (value == null)
			return this;
		AnalyzedExpression<JsonNode> rewritten = rewriter.rewrite(value);
		return rewritten == value ? this : new IdentifierKeyFieldConstruction<>(jsonProvider, key, rewritten, version, valueOutputIndex);
	}

	@Override
	public void evaluate(StackFrame frame, JsonNode in, FieldConsumer<JsonNode> consumer) throws JsonQueryException {
		if (value == null) {
			PathOperations.resolveObjectField(jsonProvider, in, UntrackedPath.getInstance(), (v, path) -> consumer.accept(key, v), key, false, version);
		} else {
			Memory memory = frame.getEnclosingMemory();
			value.apply(frame, in, UntrackedPath.getInstance(), (v, opath) -> {
				memory.countOutput(valueOutputIndex);
				consumer.accept(key, v);
			});
		}
	}
}
