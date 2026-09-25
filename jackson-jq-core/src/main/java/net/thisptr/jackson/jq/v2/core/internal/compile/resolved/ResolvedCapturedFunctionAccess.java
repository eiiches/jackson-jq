package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.FunctionDependsOnInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedCapturedFunctionAccess<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final BindContext<JsonNode> bindContext;
	private final String name;
	private final int closureSlot;
	private final int frameClosureSlot;
	private final List<AnalyzedExpression<JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final @Nullable FunctionDependsOnInfo info;

	public ResolvedCapturedFunctionAccess(BindContext<JsonNode> bindContext, String name, int closureSlot, int frameClosureSlot, List<AnalyzedExpression<JsonNode>> args, @Nullable FunctionDependsOnInfo info) {
		this.bindContext = bindContext;
		this.name = name;
		this.closureSlot = closureSlot;
		this.frameClosureSlot = frameClosureSlot;
		this.args = args;
		this.info = info;
		boolean ownInput = info == null || info.dependsOnInput();
		boolean ownExternal = info != null && info.dependsOnExternalState();
		this.dependsOnInput = ownInput || args.stream().anyMatch(AnalyzedExpression::dependsOnInput);
		this.dependsOnExternalState = ownExternal || args.stream().anyMatch(AnalyzedExpression::dependsOnExternalState);
		// Finding the callee itself already crosses a closure hop -- stay unconditionally opaque for the
		// "own" contribution (matching ResolvedCapturedVariableAccess's "defs stay conservative"
		// precedent); only args, evaluated in the caller's own frame, are ever subtractable.
		this.freeLocalSlots = FreeVariables.unionAll(args);
	}

	public String name() {
		return name;
	}

	public int closureSlot() {
		return closureSlot;
	}

	public int frameClosureSlot() {
		return frameClosureSlot;
	}

	public List<AnalyzedExpression<JsonNode>> args() {
		return args;
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
		return true;
	}

	@Override
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		List<AnalyzedExpression<JsonNode>> rewritten = ExpressionRewriter.rewriteAll(args, rewriter);
		return rewritten == args ? this : new ResolvedCapturedFunctionAccess<>(bindContext, name, closureSlot, frameClosureSlot, rewritten, info);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Closure closure = (Closure) frame.get(frameClosureSlot);
		Function factory = closure != null ? (Function) closure.get(closureSlot) : null;
		if (factory == null) {
			throw new JsonQueryException("Function " + name + " is not defined");
		}
		factory.<StackFrame, JsonNode>bind(bindContext, new java.util.ArrayList<>(args)).apply(frame, in, path, output);
	}
}
