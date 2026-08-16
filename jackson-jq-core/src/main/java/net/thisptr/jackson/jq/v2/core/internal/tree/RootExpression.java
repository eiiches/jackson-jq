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
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class RootExpression<JsonNode> implements Expression<JsonNode> {
	private final int frameSize;
	private final Expression<JsonNode> inner;
	private final Map<String, Supplier<JsonNode>> defaultVariables;
	private final Map<FunctionNameAndArity, FunctionFactory> defaultFunctionFactories;
	private final Map<String, List<Integer>> variableSlots;
	private final Map<FunctionNameAndArity, List<Integer>> functionSlots;
	private final Set<String> validVariables;
	private final Set<FunctionNameAndArity> validFunctions;

	public RootExpression(int frameSize, Expression<JsonNode> inner) {
		this(frameSize, inner, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptySet());
	}

	public RootExpression(int frameSize, Expression<JsonNode> inner,
			Map<String, Supplier<JsonNode>> defaultVariables,
			Map<FunctionNameAndArity, FunctionFactory> defaultFunctionFactories,
			Map<String, List<Integer>> variableSlots,
			Map<FunctionNameAndArity, List<Integer>> functionSlots,
			Set<String> validVariables,
			Set<FunctionNameAndArity> validFunctions) {
		this.frameSize = frameSize;
		this.inner = inner;
		this.defaultVariables = Collections.unmodifiableMap(new HashMap<>(defaultVariables));
		this.defaultFunctionFactories = Collections.unmodifiableMap(new HashMap<>(defaultFunctionFactories));
		this.variableSlots = copySlotMap(variableSlots);
		this.functionSlots = copySlotMap(functionSlots);
		this.validVariables = Collections.unmodifiableSet(new HashSet<>(validVariables));
		this.validFunctions = Collections.unmodifiableSet(new HashSet<>(validFunctions));
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
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		apply(parentFrame, in, path, output, requirePath, JsonQueryBindings.empty());
	}

	public void apply(JsonNode in, JsonQueryBindings<JsonNode> bindings, PathOutput<JsonNode> output) throws JsonQueryException {
		apply(null, in, null, output, false, bindings);
	}

	private void apply(ExecutionStack<JsonNode>.@Nullable Frame parentFrame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, boolean requirePath, JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		validateBindings(bindings);
		ExecutionStack<JsonNode>.Frame rootFrame = parentFrame != null
				? parentFrame.getStack().pushFrame(parentFrame, frameSize)
				: new ExecutionStack<JsonNode>().pushFrame(null, frameSize);
		try {
			initializeFrame(rootFrame, bindings);
			inner.apply(rootFrame, in, path, output, requirePath);
		} finally {
			rootFrame.getStack().popFrame();
		}
	}

	private void validateBindings(JsonQueryBindings<JsonNode> bindings) throws JsonQueryException {
		for (String name : bindings.variables().keySet()) {
			if (!validVariables.contains(name))
				throw new JsonQueryException("Variable $" + name + " cannot be overridden because it was not defined when the query was compiled");
		}
		for (FunctionNameAndArity key : bindings.functionFactories().keySet()) {
			if (!validFunctions.contains(key))
				throw new JsonQueryException("Function " + key + " cannot be overridden because it was not defined when the query was compiled");
		}
	}

	private void initializeFrame(ExecutionStack<JsonNode>.Frame frame, JsonQueryBindings<JsonNode> bindings) {
		for (Map.Entry<String, List<Integer>> entry : variableSlots.entrySet()) {
			String name = entry.getKey();
			Supplier<JsonNode> supplier;
			if (bindings.variables().containsKey(name)) {
				JsonNode value = bindings.variables().get(name);
				supplier = () -> value;
			} else {
				supplier = defaultVariables.get(name);
			}
			if (supplier != null) {
				for (int slot : entry.getValue())
					frame.set(slot, supplier);
			}
		}
		for (Map.Entry<FunctionNameAndArity, List<Integer>> entry : functionSlots.entrySet()) {
			FunctionFactory factory = bindings.functionFactories().containsKey(entry.getKey())
					? bindings.functionFactories().get(entry.getKey())
					: defaultFunctionFactories.get(entry.getKey());
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
