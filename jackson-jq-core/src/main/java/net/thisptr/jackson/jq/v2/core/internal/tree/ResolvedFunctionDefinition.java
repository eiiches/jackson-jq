package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.compile.Closure;
import net.thisptr.jackson.jq.v2.core.internal.compile.ClosureSpec;
import net.thisptr.jackson.jq.v2.core.internal.compile.Compiler;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedFunctionDefinition<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	private final int slot;
	private final ClosureSpec closureSpec;

	@Override
	public Cardinality getCardinality() {
		return Cardinality.ZERO;
	}

	private final int fnSize;
	private final List<String> paramNames;
	private final List<Integer> paramSlots;
	private final Expression<StackFrame, JsonNode> resolvedBody;
	private final int ownClosureSlot;
	private final int definerClosureSlot;
	private final Set<Integer> freeLocalSlots;
	private final boolean hasOpaqueVariableReference;

	public ResolvedFunctionDefinition(int slot, ClosureSpec closureSpec, int fnSize, List<String> paramNames, List<Integer> paramSlots, Expression<StackFrame, JsonNode> resolvedBody, int ownClosureSlot, int definerClosureSlot) {
		this.slot = slot;
		this.closureSpec = closureSpec;
		this.fnSize = fnSize;
		this.paramNames = paramNames;
		this.paramSlots = paramSlots;
		this.resolvedBody = resolvedBody;
		this.ownClosureSlot = ownClosureSlot;
		this.definerClosureSlot = definerClosureSlot;

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

	public Expression<StackFrame, JsonNode> resolvedBody() {
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
	public void apply(StackFrame frame, JsonNode in, @Nullable Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		Closure[] closureHolder = new Closure[1];
		Function factory = new Function() {
			@Override
			@SuppressWarnings("unchecked")
			public <Context, N> Expression<Context, N> bindArguments(JsonProvider<N> jp, List<Expression<Context, N>> fnArgs, Version version) {
				Expression<StackFrame, N> effectiveBody = (Expression<StackFrame, N>) (Expression<?, ?>) resolvedBody;
				List<Expression<StackFrame, N>> effectiveFnArgs = (List<Expression<StackFrame, N>>) (List<?>) fnArgs;
				return (callerFrame, input, path, out) -> {
					StackFrame effectiveCallerFrame = (StackFrame) callerFrame;
					Closure effectiveClosure = closureHolder[0];
					StackFrame fnFrame = effectiveCallerFrame.getEnclosingMemory().pushFrame(fnSize);
					fnFrame.set(ownClosureSlot, effectiveClosure);
					try {
						Compiler.bindAndApply(effectiveCallerFrame, fnFrame, paramNames, paramSlots, effectiveFnArgs, input, path, out, (execFrame) -> {
							effectiveBody.apply(execFrame, input, path, out);
						});
					} finally {
						fnFrame.getEnclosingMemory().popFrame();
					}
				};
			}
		};
		frame.set(slot, factory);
		closureHolder[0] = closureSpec.buildClosure(frame, definerClosureSlot);
	}
}
