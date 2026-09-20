package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.FunctionDependsOnInfo;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedLocalFunctionAccess<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final BindContext<JsonNode> bindContext;
	private final String name;
	private final int slot;
	private final List<Expression<StackFrame, JsonNode>> args;
	private final boolean dependsOnInput;
	private final boolean dependsOnExternalState;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;
	private final @Nullable FunctionDependsOnInfo info;

	public ResolvedLocalFunctionAccess(BindContext<JsonNode> bindContext, String name, int slot, List<Expression<StackFrame, JsonNode>> args, @Nullable FunctionDependsOnInfo info) {
		this.bindContext = bindContext;
		this.name = name;
		this.slot = slot;
		this.args = args;
		this.info = info;
		boolean ownInput = info == null || info.dependsOnInput();
		boolean ownExternal = info == null || info.dependsOnExternalState();
		this.dependsOnInput = ownInput || args.stream().anyMatch(Expression::dependsOnInput);
		this.dependsOnExternalState = ownExternal || args.stream().anyMatch(Expression::dependsOnExternalState);
		// The callee lives in the same frame this call runs in. Its own slot is a dependency too: evaluating
		// the call in the folder's empty frame would otherwise turn the speculative "not defined" failure
		// into a folded jq error before the enclosing def has installed the function.
		Set<Integer> free = new HashSet<>(info != null ? info.freeLocalSlots() : Collections.emptySet());
		free.add(slot);
		free.addAll(FreeVariables.unionAll(args));
		this.freeLocalSlots = free;
		this.hasOpaqueVariableReference = info == null || info.hasOpaqueVariableReference() || FreeVariables.anyOpaqueIn(args);
	}

	public String name() {
		return name;
	}

	public int slot() {
		return slot;
	}

	public List<Expression<StackFrame, JsonNode>> args() {
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
		return hasOpaqueVariableReference;
	}

	@Override
	public Expression<StackFrame, JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		List<Expression<StackFrame, JsonNode>> rewritten = ExpressionRewriter.rewriteAll(args, rewriter);
		return rewritten == args ? this : new ResolvedLocalFunctionAccess<>(bindContext, name, slot, rewritten, info);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Function factory = (Function) frame.get(slot);
		if (factory == null)
			throw new JsonQueryException("Function " + name + " is not defined");
		factory.bind(bindContext, args).apply(frame, in, ipath, output);
	}
}
