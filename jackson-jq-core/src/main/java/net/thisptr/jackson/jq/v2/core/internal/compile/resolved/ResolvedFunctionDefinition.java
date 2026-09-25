package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.ExpressionPropertiesUtils;
import net.thisptr.jackson.jq.v2.core.internal.memory.Closure;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.tree.ExpressionRewriter;
import net.thisptr.jackson.jq.v2.core.internal.tree.RewritableExpression;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class ResolvedFunctionDefinition<JsonNode> implements RewritableExpression<JsonNode>, FreeVariables {
	private final int slot;
	private final ClosureSpec closureSpec;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ZERO;
	}

	private final int fnSize;
	private final List<String> paramNames;
	private final List<Integer> paramSlots;
	private final AnalyzedExpression<JsonNode> resolvedBody;
	private final int ownClosureSlot;
	private final int definerClosureSlot;
	// Whether each execution of this body draws on RuntimeOptions#setMaxUserDefinedFunctionCalls. Set by the
	// compiler (CompileContext#metersRuntimeBudgets): true for a `def` the caller wrote, false for
	// one the engine brought along inside a module or a jq-library body, which compile to this same node.
	private final boolean metered;
	// The frame slot a tail call in this body leaves its TailCallJump in, or NO_TAIL_CALL when the body holds
	// none. Only a body that holds one is run inside the loop that drains them, so a def without one keeps
	// exactly the shape it had before tail calls existed.
	private final int tailCallSlot;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ResolvedFunctionDefinition(int slot, ClosureSpec closureSpec, int fnSize, List<String> paramNames, List<Integer> paramSlots, AnalyzedExpression<JsonNode> resolvedBody, int ownClosureSlot, int definerClosureSlot, boolean metered, int tailCallSlot) {
		this.slot = slot;
		this.closureSpec = closureSpec;
		this.fnSize = fnSize;
		this.paramNames = paramNames;
		this.paramSlots = paramSlots;
		this.resolvedBody = resolvedBody;
		this.ownClosureSlot = ownClosureSlot;
		this.definerClosureSlot = definerClosureSlot;
		this.metered = metered;
		this.tailCallSlot = tailCallSlot;
		// Capturing a variable directly off the enclosing frame (isLocalInParent) is a plain local-slot
		// read from this node's own perspective -- subtractable by an enclosing `as $x | ...`, just like
		// ResolvedLocalVariableAccess. Reaching one further via the enclosing frame's own closure is a
		// second hop, not renumberable here without extra bookkeeping -- stays opaque, matching
		// ResolvedCapturedVariableAccess's "defs stay conservative" precedent.
		Set<Integer> free = new HashSet<>();
		@Var boolean opaque = false;
		for (ClosureSpec.CapturedVariableRef ref : closureSpec.capturedVariables()) {
			if (ref.isLocalInParent) {
				free.add(ref.parentSlot);
			} else {
				opaque = true;
			}
		}
		this.freeLocalSlots = free;
		this.hasOpaqueVariableReference = opaque;
	}

	public int slot() {
		return slot;
	}

	public ClosureSpec closureSpec() {
		return closureSpec;
	}

	public int fnSize() {
		return fnSize;
	}

	public List<String> paramNames() {
		return paramNames;
	}

	public List<Integer> paramSlots() {
		return paramSlots;
	}

	public AnalyzedExpression<JsonNode> resolvedBody() {
		return resolvedBody;
	}

	public int ownClosureSlot() {
		return ownClosureSlot;
	}

	public int definerClosureSlot() {
		return definerClosureSlot;
	}

	// Installing a closure only reads already-computed values out of the enclosing frame -- it never
	// itself reads `.`/ipath or touches external state (whatever the body does when later invoked is a
	// separate concern, tracked via CompileContext.recordFunctionDependsOnInfo/FunctionDependsOnInfo for
	// call sites to consult).
	@Override
	public boolean dependsOnInput() {
		return false;
	}

	@Override
	public boolean dependsOnExternalState() {
		return false;
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
	public AnalyzedExpression<JsonNode> rewriteChildren(ExpressionRewriter<JsonNode> rewriter) {
		AnalyzedExpression<JsonNode> rewritten = rewriter.rewrite(resolvedBody);
		return rewritten == resolvedBody
				? this
				: new ResolvedFunctionDefinition<>(slot, closureSpec, fnSize, paramNames, paramSlots, rewritten, ownClosureSlot, definerClosureSlot, metered, tailCallSlot);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Closure[] closureHolder = new Closure[1];
		Function factory = new Instance(closureHolder);
		frame.set(slot, factory);
		closureHolder[0] = closureSpec.buildClosure(frame, definerClosureSlot);
	}

	/**
	 * One reach of this {@code def}: the {@link Function} installed in the definer's slot, closed over the
	 * {@link Closure} snapshotted at that moment.
	 * <p>
	 * It is also the {@link TailCallTarget} a tail call to this definition names, which is what lets
	 * {@link #activate} start another body without going back through {@link #bind}.
	 */
	private final class Instance implements Function, TailCallTarget {
		// A one-element holder rather than the Closure itself: apply() must install this Function in the
		// definer's slot *before* building the closure, so that a def can capture itself, and the closure is
		// filled in synchronously right after -- before anything could invoke the function.
		private final Closure[] closureHolder;

		Instance(Closure[] closureHolder) {
			this.closureHolder = closureHolder;
		}

		@Override
		public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
			return ExpressionPropertiesUtils.forwardAll(
					resolvedBody.getCardinality(),
					resolvedBody.dependsOnInput(),
					resolvedBody.dependsOnExternalState(),
					arguments);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <Context extends RuntimeContext, N> Expression<Context, N> bind(BindContext<N> bindCtx, List<Expression<Context, N>> fnArgs) {
			List<AnalyzedExpression<N>> effectiveFnArgs = (List<AnalyzedExpression<N>>) (List<?>) fnArgs;
			return (callerFrame, input, path, out) -> {
				StackFrame effectiveCallerFrame = (StackFrame) callerFrame;
				Memory memory = effectiveCallerFrame.getEnclosingMemory();
				// One body execution per combination of values the $-parameters' arguments produce, each
				// getting a frame of its own -- which is why one combination can never see another's slots.
				Compiler.bindParameters(effectiveCallerFrame, paramNames, effectiveFnArgs, input, path,
						(arguments) -> activate(memory, this, arguments, input, path, out));
			};
		}

		@Override
		public int frameSize() {
			return fnSize;
		}

		@Override
		public int tailCallSlot() {
			return tailCallSlot;
		}

		@Override
		public void install(StackFrame frame, Object[] arguments) {
			frame.set(ownClosureSlot, closureHolder[0]);
			for (int i = 0; i < arguments.length; i++)
				frame.set(paramSlots.get(i), arguments[i]);
		}

		// The body is compiled for this node's JsonNode, and every caller reaches it with values of that same
		// type; only the Object-typed tail-call plumbing, which is deliberately not generic, loses track of
		// that.
		//
		// NullAway: `in` is a JsonNode, and a provider that spells JSON null as Java null hands one in
		// legitimately -- the same laundering StackFrameValues#jsonNull does. Expression#apply has always
		// accepted it; only this method's Object-typed signature makes the nullness visible to the checker.
		@Override
		@SuppressWarnings({ "unchecked", "NullAway" })
		public void runBody(StackFrame frame, @Nullable Object in, Object ipath, Object output) throws JsonQueryException {
			// Charged per body execution, so a tail-recursive loop spends the budget once per iteration and
			// RuntimeOptions#setMaxUserDefinedFunctionCalls still bounds a runaway recursion.
			if (metered)
				frame.getEnclosingMemory().countUserDefinedFunctionCall();
			AnalyzedExpression<Object> body = (AnalyzedExpression<Object>) resolvedBody;
			body.apply(frame, in, (Path<Object>) ipath, (Output<Object>) output);
		}
	}

	/**
	 * Runs one body execution, and then whatever tail calls it leaves behind, until one leaves none.
	 * <p>
	 * Exactly one frame is live throughout and every body after the first runs in this same Java frame, which
	 * is the point: the chain of frames an ordinary call leaves behind is what runs the stack out after a few
	 * hundred iterations. A tail call to the body already running needs no new frame at all -- the call site
	 * has written the new arguments into the parameter slots it already has -- so a self-recursive loop
	 * allocates nothing per iteration. A tail call to a different {@code def} needs a different frame shape,
	 * so it replaces the frame rather than stacking one on top.
	 * <p>
	 * There is one loop for the whole chain, never one per body: that is what keeps a mutual {@code a -> b ->
	 * a} recursion flat instead of growing a Java frame per hop.
	 * <p>
	 * A call in tail position only becomes a tail call when everything still in progress to its left emits at
	 * most one value (see {@code Compiler}'s tail-position rules), so by the time a jump arrives here there is
	 * provably nothing left for the unwound Java frames to have done.
	 */
	private static void activate(Memory memory, TailCallTarget initial, Object[] arguments, @Nullable Object in, Object ipath, Object output) throws JsonQueryException {
		@Var TailCallTarget target = initial;
		@Var StackFrame frame = memory.pushFrame(target.frameSize());
		try {
			target.install(frame, arguments);
			if (target.tailCallSlot() == TailCallTarget.NO_TAIL_CALL) {
				target.runBody(frame, in, ipath, output);
				return;
			}
			TailCallJump jump = new TailCallJump();
			frame.set(target.tailCallSlot(), jump);
			@Var Object nextIn = in;
			@Var Object nextPath = ipath;
			@Var Object nextOutput = output;
			while (true) {
				jump.arm(target);
				target.runBody(frame, nextIn, nextPath, nextOutput);
				if (!jump.isPending())
					return;
				TailCallTarget next = jump.target();
				Object[] nextArguments = jump.arguments();
				nextIn = jump.input();
				nextPath = jump.ipath();
				nextOutput = jump.output();
				jump.clear();
				if (next != target) {
					target = next;
					memory.popFrame();
					frame = memory.pushFrame(target.frameSize());
					target.install(frame, nextArguments);
					if (target.tailCallSlot() != TailCallTarget.NO_TAIL_CALL)
						frame.set(target.tailCallSlot(), jump);
				}
			}
		} finally {
			memory.popFrame();
		}
	}
}
