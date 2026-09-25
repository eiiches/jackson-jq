package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ObjectConstruction<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	private final List<FieldConstruction<JsonNode>> fields;

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(fields, FieldConstruction::getCardinality);
	}

	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ObjectConstruction(JsonProvider<JsonNode> jsonProvider, List<FieldConstruction<JsonNode>> fields) {
		this.jsonProvider = jsonProvider;
		this.fields = List.copyOf(fields);
		this.dependsOnInput = fields.stream().anyMatch(FieldConstruction::dependsOnInput);
		this.dependsOnExternalState = fields.stream().anyMatch(FieldConstruction::dependsOnExternalState);
		Set<Integer> slots = new HashSet<>();
		for (FieldConstruction<JsonNode> field : fields)
			slots.addAll(field.freeLocalSlots());
		this.freeLocalSlots = Set.copyOf(slots);
		this.hasOpaqueVariableReference = fields.stream().anyMatch(FieldConstruction::hasOpaqueVariableReference);
	}

	public List<FieldConstruction<JsonNode>> fields() {
		return fields;
	}

	@Override
	public boolean dependsOnInput() {
		return dependsOnInput;
	}

	@Override
	public boolean dependsOnExternalState() {
		return dependsOnExternalState;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return freeLocalSlots;
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return hasOpaqueVariableReference;
	}

	@Override
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		@Var List<FieldConstruction<JsonNode>> rewritten = null;
		for (int i = 0; i < fields.size(); i++) {
			FieldConstruction<JsonNode> field = fields.get(i);
			FieldConstruction<JsonNode> replacement = field.rewriteExpressions(rewriter);
			if (rewritten == null && replacement != field)
				rewritten = new ArrayList<>(fields);
			if (rewritten != null)
				rewritten.set(i, replacement);
		}
		return rewritten == null ? this : new ObjectConstruction<>(jsonProvider, rewritten);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Map<String, JsonNode> tmp = new LinkedHashMap<>(fields.size());
		applyRecursive(jsonProvider, frame, in, output, fields, tmp);
	}

	private static <JsonNode> void applyRecursive(JsonProvider<JsonNode> jsonProvider, StackFrame frame, JsonNode in, Output<JsonNode> output, List<FieldConstruction<JsonNode>> fields, Map<String, JsonNode> tmp) throws JsonQueryException {
		if (fields.isEmpty()) {
			output.emit(jsonProvider.createObject(tmp), UntrackedPath.getInstance());
			return;
		}
		fields.get(0).evaluate(frame, in, (k, v) -> {
			tmp.put(k, v);
			applyRecursive(jsonProvider, frame, in, output, fields.subList(1, fields.size()), tmp);
			tmp.remove(k);
		});
	}
}
