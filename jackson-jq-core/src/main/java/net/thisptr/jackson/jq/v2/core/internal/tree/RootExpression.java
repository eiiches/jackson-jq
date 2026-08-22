package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.JsonQueryBindings;
import net.thisptr.jackson.jq.v2.core.internal.Memory;
import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootExpression<JsonNode> implements Expression<StackFrame, JsonNode> {
	private final int frameSize;

	@Override
	public Cardinality getCardinality() {
		return inner.getCardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return inner.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return inner.dependsOnExternalState();
	}

	// Size of the StackMemory global-slots array this compiled query needs -- one slot per referenced
	// declareVariable/declareFunction name/signature (see CompileContext#getGlobalCount).
	private final int globalCount;
	private final Expression<StackFrame, JsonNode> inner;
	// Names/signatures with a fixed, compile-time-baked value
	// (defineVariable/defineConstant/defineFunction/defineJqFunction)
	// -- used only to pick the right validateBindings() error message; the actual values are already bound
	// directly into `inner`'s tree, not stored here.
	private final Set<String> definedVariables;
	private final Set<FunctionSignature> definedFunctions;
	// Global-slot index for every declared name/signature this compiled query actually references (see
	// CompileContext#globalVariableIndices/globalFunctionIndices) -- bindings MUST supply all of these,
	// since (unlike defineVariable/defineFunction) a declared name has no compiled-in default to fall back
	// on.
	private final Map<String, Integer> globalVariableIndices;
	private final Map<FunctionSignature, Integer> globalFunctionIndices;
	// The full set of names/signatures declared on the Environment (via declareVariable/declareFunction) --
	// bindings may supply any of these, whether or not this particular compiled query happens to reference
	// them.
	private final Set<String> declaredVariables;
	private final Set<FunctionSignature> declaredFunctions;
	private final Map<FunctionSignature, Integer> rootFunctionSlots;

	public RootExpression(int frameSize, Expression<StackFrame, JsonNode> inner) {
		this(frameSize, 0, inner, Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
	}

	public RootExpression(int frameSize, int globalCount, Expression<StackFrame, JsonNode> inner,
						  Set<String> definedVariables,
						  Set<FunctionSignature> definedFunctions,
						  Map<String, Integer> globalVariableIndices,
						  Map<FunctionSignature, Integer> globalFunctionIndices,
						  Set<String> declaredVariables,
						  Set<FunctionSignature> declaredFunctions,
						  Map<FunctionSignature, Integer> rootFunctionSlots) {
		this.frameSize = frameSize;
		this.globalCount = globalCount;
		this.inner = inner;
		this.definedVariables = Collections.unmodifiableSet(new HashSet<>(definedVariables));
		this.definedFunctions = Collections.unmodifiableSet(new HashSet<>(definedFunctions));
		this.globalVariableIndices = Collections.unmodifiableMap(new HashMap<>(globalVariableIndices));
		this.globalFunctionIndices = Collections.unmodifiableMap(new HashMap<>(globalFunctionIndices));
		this.declaredVariables = Collections.unmodifiableSet(new HashSet<>(declaredVariables));
		this.declaredFunctions = Collections.unmodifiableSet(new HashSet<>(declaredFunctions));
		this.rootFunctionSlots = Collections.unmodifiableMap(new HashMap<>(rootFunctionSlots));
	}

	public int frameSize() {
		return frameSize;
	}

	public Expression<StackFrame, JsonNode> inner() {
		return inner;
	}

	@Override
	public void apply(StackFrame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		apply(parentFrame, in, path, output, JsonQueryBindings.empty());
	}

	public void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, Output<JsonNode> output) throws JsonQueryException {
		apply((StackFrame) null, in, null, output, bindings);
	}

	public void apply(JsonNode in, Output<JsonNode> output) throws JsonQueryException {
		apply(in, JsonQueryBindings.empty(), output);
	}

	private void apply(@Nullable StackFrame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, Output<JsonNode> output, JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		validateBindings(bindings);
		Memory memory = parentFrame != null ? parentFrame.getEnclosingMemory() : new Memory(globalCount);
		StackFrame rootFrame = memory.pushFrame(frameSize);
		try {
			initializeGlobals(memory, bindings);
			inner.apply(rootFrame, in, path, output);
		} finally {
			memory.popFrame();
		}
	}

	/**
	 * Runs this module's own defining top-level statements once (each {@code def} builds and registers its
	 * own, correctly closure-bound {@link Function} into a fresh root frame -- exactly as an ordinary,
	 * non-exported call to {@link net.thisptr.jackson.jq.v2.core.internal.compile.Compiler#compile} would),
	 * and returns the resulting {@link Function} for every module-level {@code def} this was compiled with
	 * {@link net.thisptr.jackson.jq.v2.core.internal.compile.Compiler#compileModule}. Used by
	 * {@code FileSystemModuleLoader} to harvest a file-based module's exported functions; ordinary
	 * (non-module) compiles carry an empty {@code rootFunctionSlots} map and this always returns empty.
	 */
	public Map<FunctionSignature, Function> applyForModuleExports(JsonNode in) throws JsonQueryException {
		Memory memory = new Memory(globalCount);
		StackFrame rootFrame = memory.pushFrame(frameSize);
		try {
			initializeGlobals(memory, JsonQueryBindings.empty());
			inner.apply(rootFrame, in, null, (v, p) -> {
			});
			Map<FunctionSignature, Function> result = new HashMap<>();
			for (Map.Entry<FunctionSignature, Integer> entry : rootFunctionSlots.entrySet()) {
				Object raw = rootFrame.get(entry.getValue());
				if (raw instanceof Function) {
					result.put(entry.getKey(), (Function) raw);
				}
			}
			return result;
		} finally {
			memory.popFrame();
		}
	}

	private void validateBindings(JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		for (String name : bindings.variables().keySet()) {
			if (declaredVariables.contains(name))
				continue;
			if (definedVariables.contains(name))
				throw new JsonQueryException("Variable $" + name + " cannot be overridden because it has a fixed value in the Environment");
			throw new JsonQueryException("Variable $" + name + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (FunctionSignature key : bindings.functions().keySet()) {
			if (declaredFunctions.contains(key))
				continue;
			if (definedFunctions.contains(key))
				throw new JsonQueryException("Function " + key + " cannot be overridden because it has a fixed value in the Environment");
			throw new JsonQueryException("Function " + key + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (String name : globalVariableIndices.keySet()) {
			if (!bindings.variables().containsKey(name))
				throw new JsonQueryException("Variable $" + name + " must be supplied when calling apply(), because it was declared without a value in the Environment");
		}
		for (FunctionSignature key : globalFunctionIndices.keySet()) {
			if (!bindings.functions().containsKey(key))
				throw new JsonQueryException("Function " + key + " must be supplied when calling apply(), because it was declared without a value in the Environment");
		}
	}

	private void initializeGlobals(Memory memory, JsonQueryBindings<JsonNode> bindings) {
		for (Map.Entry<String, Integer> entry : globalVariableIndices.entrySet()) {
			Supplier<JsonNode> supplier = bindings.variables().get(entry.getKey());
			if (supplier != null)
				memory.setGlobal(entry.getValue(), supplier);
		}
		for (Map.Entry<FunctionSignature, Integer> entry : globalFunctionIndices.entrySet()) {
			Function factory = bindings.functions().get(entry.getKey());
			if (factory != null)
				memory.setGlobal(entry.getValue(), factory);
		}
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
