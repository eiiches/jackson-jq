package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

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
	public void apply(StackFrame parentFrame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		// Nested use: the enclosing invocation's Memory -- and hence its limits -- is reused, so the
		// RuntimeLimits argument is never read.
		apply(parentFrame, in, path, output, null, parentFrame.getRuntimeLimits());
	}

	public void apply(JsonNode in, RuntimeLimits runtimeLimits, RuntimeBindings<JsonNode> bindings, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply((StackFrame) null, in, UntrackedPath.getInstance(), (v, p) -> output.accept(v), bindings, runtimeLimits);
	}

	private void apply(@Nullable StackFrame parentFrame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output, @Nullable RuntimeBindings<JsonNode> bindings, RuntimeLimits runtimeLimits) throws JsonQueryException {
		validateBindings(bindings);
		Memory memory = parentFrame != null ? parentFrame.getEnclosingMemory() : new Memory(globalCount, runtimeLimits);
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
	 * {@link net.thisptr.jackson.jq.v2.core.internal.compile.Compiler#compileModule}. Used by the module
	 * resolver to harvest an imported module's exported functions; ordinary
	 * (non-module) compiles carry an empty {@code rootFunctionSlots} map and this always returns empty.
	 */
	public Map<FunctionSignature, Function> applyForModuleExports(JsonNode in) throws JsonQueryException {
		Memory memory = new Memory(globalCount);
		StackFrame rootFrame = memory.pushFrame(frameSize);
		try {
			initializeGlobals(memory, null);
			inner.apply(rootFrame, in, UntrackedPath.getInstance(), (v, p) -> {
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

	private void validateBindings(@Nullable RuntimeBindings<JsonNode> bindings) throws JsonQueryException {
		Map<String, Supplier<JsonNode>> variables = bindings != null ? bindings.getVariables() : Collections.emptyMap();
		Map<FunctionSignature, Function> functions = bindings != null ? bindings.getFunctions() : Collections.emptyMap();
		for (String name : variables.keySet()) {
			if (declaredVariables.contains(name))
				continue;
			if (definedVariables.contains(name))
				throw new JsonQueryException("Variable $" + name + " cannot be overridden because it has a fixed value in the Environment");
			throw new JsonQueryException("Variable $" + name + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (FunctionSignature key : functions.keySet()) {
			if (declaredFunctions.contains(key))
				continue;
			if (definedFunctions.contains(key))
				throw new JsonQueryException("Function " + key + " cannot be overridden because it has a fixed value in the Environment");
			throw new JsonQueryException("Function " + key + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (String name : globalVariableIndices.keySet()) {
			if (!variables.containsKey(name))
				throw new JsonQueryException("Variable $" + name + " must be supplied when calling apply(), because it was declared without a value in the Environment");
		}
		for (FunctionSignature key : globalFunctionIndices.keySet()) {
			if (!functions.containsKey(key))
				throw new JsonQueryException("Function " + key + " must be supplied when calling apply(), because it was declared without a value in the Environment");
		}
	}

	private void initializeGlobals(Memory memory, @Nullable RuntimeBindings<JsonNode> bindings) {
		if (bindings == null)
			return;
		for (Map.Entry<String, Integer> entry : globalVariableIndices.entrySet()) {
			Supplier<JsonNode> supplier = bindings.getVariables().get(entry.getKey());
			if (supplier != null)
				memory.setGlobal(entry.getValue(), supplier);
		}
		for (Map.Entry<FunctionSignature, Integer> entry : globalFunctionIndices.entrySet()) {
			Function factory = bindings.getFunctions().get(entry.getKey());
			if (factory != null)
				memory.setGlobal(entry.getValue(), factory);
		}
	}
}
