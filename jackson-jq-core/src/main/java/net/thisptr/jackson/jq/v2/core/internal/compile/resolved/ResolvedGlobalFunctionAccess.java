package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Call to an {@code EnvironmentBuilder.declareFunction}-registered function -- no compile-time
 * implementation, so the {@link Function} is read from {@code StackFrame.getEnclosingMemory()}'s flat
 * global-slots array (populated once per top-level {@code apply()} call from {@code JsonQueryBindings})
 * and bound against the call's arguments fresh on every evaluation.
 */
public class ResolvedGlobalFunctionAccess<JsonNode> implements Expression<StackFrame, JsonNode> {
	private final JsonProvider<JsonNode> jsonProvider;
	private final Version version;
	private final String name;
	private final int globalIndex;
	private final List<Expression<StackFrame, JsonNode>> args;

	public ResolvedGlobalFunctionAccess(JsonProvider<JsonNode> jsonProvider, Version version, String name, int globalIndex, List<Expression<StackFrame, JsonNode>> args) {
		this.jsonProvider = jsonProvider;
		this.version = version;
		this.name = name;
		this.globalIndex = globalIndex;
		this.args = args;
	}

	public String name() {
		return name;
	}

	public List<Expression<StackFrame, JsonNode>> args() {
		return args;
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Function factory = (Function) frame.getEnclosingMemory().getGlobal(globalIndex);
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		factory.bindArguments(jsonProvider, args, version).apply(frame, in, path, output);
	}

	@Override
	public String toString() {
		return name;
	}
}
