package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public class RootExpression<JsonNode> implements Expression<StackFrame, JsonNode> {
	private static final Object[] EMPTY_GLOBALS = new Object[0];

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
	// -- used only to pick the right prepareBindings() error message; the actual values are already bound
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
		prepareBindings(null);
		apply(parentFrame.getEnclosingMemory(), in, path, output);
	}

	/**
	 * Top-level entry point. {@code globals} must have been produced by {@link #prepareBindings};
	 * {@link net.thisptr.jackson.jq.v2.core.JsonQuery#withRuntimeBindings} does so once, when the query carrying
	 * them is built.
	 */
	public void apply(JsonNode in, RuntimeLimitsImpl runtimeLimits, Object[] globals, Consumer<? super JsonNode> output) throws JsonQueryException {
		apply(new Memory(globals, runtimeLimits), in, UntrackedPath.getInstance(), (v, p) -> output.accept(v));
	}

	private void apply(Memory memory, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		StackFrame rootFrame = memory.pushFrame(frameSize);
		try {
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

	public Object @Nullable [] prepareEmptyBindings() {
		return globalCount == 0 ? EMPTY_GLOBALS : null;
	}

	public Object[] prepareBindings(@Nullable RuntimeBindings<JsonNode> bindings) throws JsonQueryException {
		Map<String, Supplier<JsonNode>> variables = bindings != null ? bindings.getVariables() : Collections.emptyMap();
		Map<FunctionSignature, Function> functions = bindings != null ? bindings.getFunctions() : Collections.emptyMap();
		Object[] globals = globalCount == 0 ? EMPTY_GLOBALS : new Object[globalCount];
		@Var int populatedGlobalCount = 0;
		if (!variables.isEmpty()) {
			for (Map.Entry<String, Supplier<JsonNode>> entry : variables.entrySet()) {
				String name = entry.getKey();
				if (declaredVariables.contains(name)) {
					Integer index = globalVariableIndices.get(name);
					if (index != null) {
						globals[index] = entry.getValue();
						populatedGlobalCount++;
					}
					continue;
				}
				if (definedVariables.contains(name))
					throw new JsonQueryException("Variable $" + name + " cannot be overridden because it has a fixed value in the Environment");
				throw new JsonQueryException("Variable $" + name + " cannot be overridden because it was not defined when the query was compiled");
			}
		}
		if (!functions.isEmpty()) {
			for (Map.Entry<FunctionSignature, Function> entry : functions.entrySet()) {
				FunctionSignature key = entry.getKey();
				if (declaredFunctions.contains(key)) {
					Integer index = globalFunctionIndices.get(key);
					if (index != null) {
						globals[index] = entry.getValue();
						populatedGlobalCount++;
					}
					continue;
				}
				if (definedFunctions.contains(key))
					throw new JsonQueryException("Function " + key + " cannot be overridden because it has a fixed value in the Environment");
				throw new JsonQueryException("Function " + key + " cannot be overridden because it was not defined when the query was compiled");
			}
		}
		if (populatedGlobalCount != globalCount) {
			if (!globalVariableIndices.isEmpty()) {
				for (Map.Entry<String, Integer> entry : globalVariableIndices.entrySet()) {
					if (globals[entry.getValue()] == null)
						throw new JsonQueryException("Variable $" + entry.getKey() + " must be supplied via withRuntimeBindings(), because it was declared without a value in the Environment");
				}
			}
			if (!globalFunctionIndices.isEmpty()) {
				for (Map.Entry<FunctionSignature, Integer> entry : globalFunctionIndices.entrySet()) {
					if (globals[entry.getValue()] == null)
						throw new JsonQueryException("Function " + entry.getKey() + " must be supplied via withRuntimeBindings(), because it was declared without a value in the Environment");
				}
			}
		}
		return globals;
	}
}
