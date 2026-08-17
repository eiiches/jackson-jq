package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.StackFrame;

public class ClosureSpec {
	public static class CapturedVariableRef {
		public final boolean isLocalInParent;
		public final int parentSlot;
		public final int targetSlot;

		public CapturedVariableRef(boolean isLocalInParent, int parentSlot, int targetSlot) {
			this.isLocalInParent = isLocalInParent;
			this.parentSlot = parentSlot;
			this.targetSlot = targetSlot;
		}
	}

	public static class CapturedFunctionRef {
		public final boolean isLocalInParent;
		public final int parentSlot;
		public final int targetSlot;

		public CapturedFunctionRef(boolean isLocalInParent, int parentSlot, int targetSlot) {
			this.isLocalInParent = isLocalInParent;
			this.parentSlot = parentSlot;
			this.targetSlot = targetSlot;
		}
	}

	private final List<CapturedVariableRef> capturedVariables;
	private final List<CapturedFunctionRef> capturedFunctions;

	public ClosureSpec(List<CapturedVariableRef> capturedVariables, List<CapturedFunctionRef> capturedFunctions) {
		this.capturedVariables = capturedVariables;
		this.capturedFunctions = capturedFunctions;
	}

	public List<CapturedVariableRef> capturedVariables() {
		return capturedVariables;
	}

	public List<CapturedFunctionRef> capturedFunctions() {
		return capturedFunctions;
	}

	/**
	 * Builds this function's own Closure by reading off {@code currentFrame} -- the frame that's live right
	 * now, at the moment this def is reached. A hop-1 entry ({@code isLocalInParent}) reads a slot directly
	 * off that frame. A hop-2+ entry reads instead from {@code currentFrame}'s own Closure, found at
	 * {@code definerClosureSlot} of that same frame (the immediately-enclosing function's own reserved slot).
	 */
	@SuppressWarnings("unchecked")
	public <JsonNode> Closure<JsonNode> buildClosure(@Nullable StackFrame<JsonNode> currentFrame, int definerClosureSlot) {
		Closure<JsonNode> closure = new Closure<>(capturedVariables.size() + capturedFunctions.size());
		@Var @Nullable Closure<JsonNode> parentClosure = null;
		@Var boolean parentClosureFetched = false;
		for (CapturedVariableRef ref : capturedVariables) {
			Object value;
			if (ref.isLocalInParent) {
				value = currentFrame != null ? currentFrame.getRawValue(ref.parentSlot) : null;
			} else {
				if (!parentClosureFetched) {
					parentClosure = currentFrame != null ? (Closure<JsonNode>) currentFrame.getRawValue(definerClosureSlot) : null;
					parentClosureFetched = true;
				}
				value = parentClosure != null ? parentClosure.getRawValue(ref.parentSlot) : null;
			}
			closure.setRawValue(ref.targetSlot, value);
		}
		for (CapturedFunctionRef ref : capturedFunctions) {
			Object value;
			if (ref.isLocalInParent) {
				value = currentFrame != null ? currentFrame.getFunctionFactory(ref.parentSlot) : null;
			} else {
				if (!parentClosureFetched) {
					parentClosure = currentFrame != null ? (Closure<JsonNode>) currentFrame.getRawValue(definerClosureSlot) : null;
					parentClosureFetched = true;
				}
				value = parentClosure != null ? parentClosure.getRawValue(ref.parentSlot) : null;
			}
			closure.setRawValue(ref.targetSlot, value);
		}
		return closure;
	}
}
