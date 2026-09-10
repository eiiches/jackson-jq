package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.CardinalityUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class ObjectConstruction<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final JsonProvider<JsonNode> jsonProvider;
	public final List<FieldConstruction<JsonNode>> fields = new ArrayList<>();

	@Override
	public Cardinality getCardinality() {
		return CardinalityUtils.multiply(fields, FieldConstruction::getCardinality);
	}

	// Fields are appended one at a time via add() after construction (see Compiler's
	// ObjectConstructionAstNode handling), so unlike every other node these can't be computed once
	// in the constructor -- they're folded in incrementally as each field arrives instead.
	private boolean dependsOnInput = false;
	private boolean dependsOnExternalState = false;
	private Set<Integer> freeLocalSlots = Collections.emptySet();
	private boolean hasOpaqueVariableReference = false;

	public ObjectConstruction(JsonProvider<JsonNode> jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public void add(FieldConstruction<JsonNode> field) {
		fields.add(field);
		dependsOnInput = dependsOnInput || field.dependsOnInput();
		dependsOnExternalState = dependsOnExternalState || field.dependsOnExternalState();
		if (!field.freeLocalSlots().isEmpty()) {
			Set<Integer> merged = new HashSet<>(freeLocalSlots);
			merged.addAll(field.freeLocalSlots());
			freeLocalSlots = merged;
		}
		hasOpaqueVariableReference = hasOpaqueVariableReference || field.hasOpaqueVariableReference();
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		@Var String sep = "";
		for (FieldConstruction<JsonNode> field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
