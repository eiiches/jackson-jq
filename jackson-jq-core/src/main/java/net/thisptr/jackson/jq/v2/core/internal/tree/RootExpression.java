package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.JsonQueryBindings;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.StackMemory;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootExpression<JsonNode> implements Expression<JsonNode> {
	private final int frameSize;
	private final Expression<JsonNode> inner;
	private final Map<String, Supplier<JsonNode>> defaultVariables;
	private final Map<FunctionSignature, Function> defaultFunctions;
	private final Map<String, List<Integer>> variableSlots;
	private final Map<FunctionSignature, List<Integer>> functionSlots;
	private final Set<String> validVariables;
	private final Set<FunctionSignature> validFunctions;
	private final Map<FunctionSignature, Integer> rootFunctionSlots;

	public RootExpression(int frameSize, Expression<JsonNode> inner) {
		this(frameSize, inner, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
	}

	public RootExpression(int frameSize, Expression<JsonNode> inner,
			Map<String, Supplier<JsonNode>> defaultVariables,
			Map<FunctionSignature, Function> defaultFunctions,
			Map<String, List<Integer>> variableSlots,
			Map<FunctionSignature, List<Integer>> functionSlots,
			Set<String> validVariables,
			Set<FunctionSignature> validFunctions,
			Map<FunctionSignature, Integer> rootFunctionSlots) {
		this.frameSize = frameSize;
		this.inner = inner;
		this.defaultVariables = Collections.unmodifiableMap(new HashMap<>(defaultVariables));
		this.defaultFunctions = Collections.unmodifiableMap(new HashMap<>(defaultFunctions));
		this.variableSlots = copySlotMap(variableSlots);
		this.functionSlots = copySlotMap(functionSlots);
		this.validVariables = Collections.unmodifiableSet(new HashSet<>(validVariables));
		this.validFunctions = Collections.unmodifiableSet(new HashSet<>(validFunctions));
		this.rootFunctionSlots = Collections.unmodifiableMap(new HashMap<>(rootFunctionSlots));
	}

	private static <K> Map<K, List<Integer>> copySlotMap(Map<K, List<Integer>> source) {
		Map<K, List<Integer>> result = new HashMap<>();
		for (Map.Entry<K, List<Integer>> entry : source.entrySet())
			result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
		return Collections.unmodifiableMap(result);
	}

	public int frameSize() {
		return frameSize;
	}

	public Expression<JsonNode> inner() {
		return inner;
	}

	@Override
	public void apply(@Nullable StackFrame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		apply(parentFrame, in, path, output, requirePath, JsonQueryBindings.empty());
	}

	public void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, PathOutput<JsonNode> output) throws JsonQueryException {
		apply(null, in, null, output, false, bindings);
	}

	private void apply(@Nullable StackFrame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath, JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		validateBindings(bindings);
		StackFrame rootFrame = parentFrame != null
				? parentFrame.getEnclosingMemory().pushFrame(frameSize)
				: new StackMemory().pushFrame(frameSize);
		try {
			initializeFrame(rootFrame, bindings);
			inner.apply(rootFrame, in, path, output, requirePath);
		} finally {
			rootFrame.getEnclosingMemory().popFrame();
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
		StackFrame rootFrame = new StackMemory().pushFrame(frameSize);
		try {
			initializeFrame(rootFrame, JsonQueryBindings.empty());
			inner.apply(rootFrame, in, null, (v, p) -> { }, false);
			Map<FunctionSignature, Function> result = new HashMap<>();
			for (Map.Entry<FunctionSignature, Integer> entry : rootFunctionSlots.entrySet()) {
				Object raw = rootFrame.get(entry.getValue());
				if (raw instanceof Function) {
					result.put(entry.getKey(), (Function) raw);
				}
			}
			return result;
		} finally {
			rootFrame.getEnclosingMemory().popFrame();
		}
	}

	private void validateBindings(JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		for (String name : bindings.variables().keySet()) {
			if (!validVariables.contains(name))
				throw new JsonQueryException("Variable $" + name + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (FunctionSignature key : bindings.functions().keySet()) {
			if (!validFunctions.contains(key))
				throw new JsonQueryException("Function " + key + " cannot be overridden because it was not defined when the query was compiled");
		}
	}

	private void initializeFrame(StackFrame frame, JsonQueryBindings<JsonNode> bindings) {
		for (Map.Entry<String, List<Integer>> entry : variableSlots.entrySet()) {
			String name = entry.getKey();
			Supplier<JsonNode> supplier = bindings.variables().containsKey(name)
					? bindings.variables().get(name)
					: defaultVariables.get(name);
			if (supplier != null) {
				for (int slot : entry.getValue())
					frame.set(slot, supplier);
			}
		}
		for (Map.Entry<FunctionSignature, List<Integer>> entry : functionSlots.entrySet()) {
			Function factory = bindings.functions().containsKey(entry.getKey())
					? bindings.functions().get(entry.getKey())
					: defaultFunctions.get(entry.getKey());
			if (factory != null) {
				for (int slot : entry.getValue())
					frame.set(slot, factory);
			}
		}
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
