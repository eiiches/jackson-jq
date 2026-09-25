package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Call to an {@code EnvironmentBuilder.declareFunction}-registered function -- no compile-time
 * implementation, so the {@link Function} is read from {@code StackFrame.getEnclosingMemory()}'s flat
 * global-slots array (prepared when the immutable query view is built and shared read-only by its invocations)
 * and bound against the call's arguments fresh on every evaluation.
 */
public class ResolvedGlobalFunctionAccess<JsonNode> implements RewritableExpression<JsonNode> {
	private final BindContext<JsonNode> bindContext;
	private final String name;
	private final int globalIndex;
	private final List<AnalyzedExpression<JsonNode>> args;

	public ResolvedGlobalFunctionAccess(BindContext<JsonNode> bindContext, String name, int globalIndex, List<AnalyzedExpression<JsonNode>> args) {
		this.bindContext = bindContext;
		this.name = name;
		this.globalIndex = globalIndex;
		this.args = args;
	}

	public String name() {
		return name;
	}

	public List<AnalyzedExpression<JsonNode>> args() {
		return args;
	}

	@Override
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		List<AnalyzedExpression<JsonNode>> rewritten = ExpressionRewriter.rewriteAll(args, rewriter);
		return rewritten == args ? this : new ResolvedGlobalFunctionAccess<>(bindContext, name, globalIndex, rewritten);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Function factory = (Function) frame.getEnclosingMemory().getGlobal(globalIndex);
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		factory.<StackFrame, JsonNode>bind(bindContext, new java.util.ArrayList<>(args)).apply(frame, in, path, output);
	}
}
