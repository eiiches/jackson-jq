package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Closure;
import net.thisptr.jackson.jq.v2.spi.ExecutionStack;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;

public class ClosureSpec {
	public static class CapturedVariableRef {
		public final boolean isLocalInParent;
		public final int parentSlot;

		public CapturedVariableRef(boolean isLocalInParent, int parentSlot) {
			this.isLocalInParent = isLocalInParent;
			this.parentSlot = parentSlot;
		}
	}

	public static class CapturedFunctionRef {
		public final boolean isLocalInParent;
		public final int parentSlot;

		public CapturedFunctionRef(boolean isLocalInParent, int parentSlot) {
			this.isLocalInParent = isLocalInParent;
			this.parentSlot = parentSlot;
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

	public <JsonNode> Closure<JsonNode> buildClosure(ExecutionStack<JsonNode>.@Nullable Frame currentFrame) {
		Closure<JsonNode> parentClosure = currentFrame != null ? currentFrame.getClosure() : null;
		Object[] vars = new Object[capturedVariables.size()];
		for (int i = 0; i < capturedVariables.size(); i++) {
			CapturedVariableRef ref = capturedVariables.get(i);
			if (ref.isLocalInParent) {
				vars[i] = currentFrame != null ? currentFrame.getValue(ref.parentSlot) : null;
			} else {
				vars[i] = parentClosure != null ? parentClosure.getVariable(ref.parentSlot) : null;
			}
		}
		FunctionFactory[] fns = new FunctionFactory[capturedFunctions.size()];
		for (int i = 0; i < capturedFunctions.size(); i++) {
			CapturedFunctionRef ref = capturedFunctions.get(i);
			if (ref.isLocalInParent) {
				fns[i] = currentFrame != null ? currentFrame.getFunctionFactory(ref.parentSlot) : null;
			} else {
				fns[i] = parentClosure != null ? parentClosure.getFunctionFactory(ref.parentSlot) : null;
			}
		}
		return new Closure<>(vars, fns);
	}
}
