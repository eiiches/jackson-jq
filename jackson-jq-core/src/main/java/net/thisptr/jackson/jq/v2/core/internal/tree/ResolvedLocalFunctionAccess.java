package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalFunctionAccess<JsonNode> implements Expression<JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int slot;
	private final List<Expression<JsonNode>> args;
	private final @Nullable FunctionFactory defaultFactory;
	private final @Nullable Function<JsonNode> defaultFunction;

	public ResolvedLocalFunctionAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int slot, List<Expression<JsonNode>> args, @Nullable FunctionFactory defaultFactory, @Nullable Function<JsonNode> defaultFunction) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.slot = slot;
		this.args = args;
		this.defaultFactory = defaultFactory;
		this.defaultFunction = defaultFunction;
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	public List<Expression<JsonNode>> args() {
		return args;
	}

	@Override
	public void apply(ExecutionStack<JsonNode>.@Nullable Frame frame, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, boolean requirePath) throws JsonQueryException {
		FunctionFactory factory = frame != null ? frame.getFunctionFactory(slot) : null;
		if (factory == null && defaultFunction != null) {
			defaultFunction.apply(frame, in, ipath, output);
			return;
		}
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		Function<JsonNode> fn = factory == defaultFactory && defaultFunction != null
				? defaultFunction : factory.createFunction(jsonProvider, args, version);
		fn.apply(frame, in, ipath, output);
	}

	@Override
	public String toString() {
		return name;
	}
}
