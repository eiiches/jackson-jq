package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;

public class CompileContext {
	private static class ScopeFrame {
		final boolean isFunctionBoundary;
		final Set<String> variables = new HashSet<>();
		final Set<FunctionNameAndArity> functions = new HashSet<>();
		final Map<String, Integer> symbolSlots = new HashMap<>();
		int nextSlot;

		final List<ClosureSpec.CapturedVariableRef> capturedVariables = new ArrayList<>();
		final Map<String, Integer> capturedVarSlots = new HashMap<>();

		final List<ClosureSpec.CapturedFunctionRef> capturedFunctions = new ArrayList<>();
		final Map<FunctionNameAndArity, Integer> capturedFnSlots = new HashMap<>();

		ScopeFrame(boolean isFunctionBoundary, int initialSlot) {
			this.isFunctionBoundary = isFunctionBoundary;
			this.nextSlot = initialSlot;
		}

		ScopeFrame copy() {
			ScopeFrame sf = new ScopeFrame(this.isFunctionBoundary, this.nextSlot);
			sf.variables.addAll(this.variables);
			sf.functions.addAll(this.functions);
			sf.symbolSlots.putAll(this.symbolSlots);
			sf.capturedVariables.addAll(this.capturedVariables);
			sf.capturedVarSlots.putAll(this.capturedVarSlots);
			sf.capturedFunctions.addAll(this.capturedFunctions);
			sf.capturedFnSlots.putAll(this.capturedFnSlots);
			return sf;
		}
	}

	private final List<ScopeFrame> scopes;

	public CompileContext() {
		this.scopes = new ArrayList<>();
		this.scopes.add(new ScopeFrame(true, 0));
	}

	private CompileContext(List<ScopeFrame> scopes) {
		this.scopes = new ArrayList<>();
		for (ScopeFrame sf : scopes) {
			this.scopes.add(sf.copy());
		}
	}

	public CompileContext copy() {
		return new CompileContext(scopes);
	}

	public void pushLocalScope() {
		int currentSlot = scopes.isEmpty() ? 0 : scopes.get(scopes.size() - 1).nextSlot;
		scopes.add(new ScopeFrame(false, currentSlot));
	}

	public void pushFunctionScope() {
		scopes.add(new ScopeFrame(true, 0));
	}

	public void popScope() {
		if (scopes.size() > 1) {
			ScopeFrame popped = scopes.remove(scopes.size() - 1);
			if (!popped.isFunctionBoundary && !scopes.isEmpty()) {
				ScopeFrame top = scopes.get(scopes.size() - 1);
				if (popped.nextSlot > top.nextSlot) {
					top.nextSlot = popped.nextSlot;
				}
			}
		}
	}

	public int getSlotCount() {
		if (scopes.isEmpty())
			return 0;
		return scopes.get(scopes.size() - 1).nextSlot;
	}

	public ClosureSpec getClosureSpec() {
		if (scopes.isEmpty())
			return new ClosureSpec(Collections.emptyList(), Collections.emptyList());
		ScopeFrame top = scopes.get(scopes.size() - 1);
		return new ClosureSpec(new ArrayList<>(top.capturedVariables), new ArrayList<>(top.capturedFunctions));
	}

	public void addLocalVariable(String name) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.variables.add(name);
		getOrAssignSlotInTop(name);
	}

	public void addLocalFunction(String name, int arity) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.functions.add(FunctionNameAndArity.of(name, arity));
		getOrAssignSlotInTop(name);
	}

	public int getOrAssignSlotInTop(String name) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		Integer slot = top.symbolSlots.get(name);
		if (slot != null)
			return slot;
		int assigned = top.nextSlot++;
		top.symbolSlots.put(name, assigned);
		return assigned;
	}

	public boolean isLocalVariable(String name) {
		return getVariableLocation(name) != null;
	}

	public boolean isLocalFunction(String name, int arity) {
		return getFunctionLocation(name, arity) != null;
	}

	public int getSlot(String name) {
		@Var SymbolLocation loc = getVariableLocation(name);
		if (loc != null)
			return loc.slot;
		loc = getFunctionLocation(name, 0);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public @Nullable SymbolLocation getVariableLocation(String name) {
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		if (current.variables.contains(name)) {
			Integer slot = current.symbolSlots.get(name);
			if (slot != null) {
				return SymbolLocation.local(slot);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			if (outer.variables.contains(name)) {
				Integer localSlot = outer.symbolSlots.get(name);
				if (localSlot != null) {
					if (!crossedFunctionBoundary) {
						return SymbolLocation.local(localSlot);
					}
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
						if (closureSlot == null) {
							closureSlot = targetFrame.capturedVariables.size();
							targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot));
							targetFrame.capturedVarSlots.put(name, closureSlot);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return SymbolLocation.captured(targetSlot);
				}
			}
			Integer existingClosureSlot = outer.capturedVarSlots.get(name);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
					if (closureSlot == null) {
						closureSlot = targetFrame.capturedVariables.size();
						targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot));
						targetFrame.capturedVarSlots.put(name, closureSlot);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}

	public @Nullable SymbolLocation getFunctionLocation(String name, int arity) {
		FunctionNameAndArity key = FunctionNameAndArity.of(name, arity);
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		if (current.functions.contains(key)) {
			Integer slot = current.symbolSlots.get(name);
			if (slot != null) {
				return SymbolLocation.local(slot);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			if (outer.functions.contains(key)) {
				Integer localSlot = outer.symbolSlots.get(name);
				if (localSlot != null) {
					if (!crossedFunctionBoundary) {
						return SymbolLocation.local(localSlot);
					}
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedFnSlots.get(key);
						if (closureSlot == null) {
							closureSlot = targetFrame.capturedFunctions.size();
							targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot));
							targetFrame.capturedFnSlots.put(key, closureSlot);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return SymbolLocation.captured(targetSlot);
				}
			}
			Integer existingClosureSlot = outer.capturedFnSlots.get(key);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedFnSlots.get(key);
					if (closureSlot == null) {
						closureSlot = targetFrame.capturedFunctions.size();
						targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot));
						targetFrame.capturedFnSlots.put(key, closureSlot);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}
}
