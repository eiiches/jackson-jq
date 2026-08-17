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
		final Map<String, Integer> variableSlots = new HashMap<>();
		final Map<FunctionNameAndArity, Integer> functionSlots = new HashMap<>();
		int nextSlot;

		// Reserved slot (within this function's own frame) that will hold this function's own Closure at
		// runtime -- the "static link" slot. Set once, right after params, before the body is compiled, so
		// that nested defs compiled while this scope is on top can reference it as a fixed, known-in-advance
		// constant. -1 until reserveClosureSlot() is called; never called for scopes that aren't the subject
		// of a `def` (the implicit root scope, or a local scope).
		int closureSlot = -1;

		// Shared index space for names captured into this function's own Closure -- one flat numbering for
		// both captured variables and captured functions, mirroring how nextSlot is already shared between
		// locals of both kinds.
		int nextClosureSlot;

		final List<ClosureSpec.CapturedVariableRef> capturedVariables = new ArrayList<>();
		final Map<String, Integer> capturedVarSlots = new HashMap<>();
		final Set<String> capturedGlobalVariables = new HashSet<>();

		final List<ClosureSpec.CapturedFunctionRef> capturedFunctions = new ArrayList<>();
		final Map<FunctionNameAndArity, Integer> capturedFnSlots = new HashMap<>();
		final Set<FunctionNameAndArity> capturedGlobalFunctions = new HashSet<>();

		ScopeFrame(boolean isFunctionBoundary, int initialSlot) {
			this.isFunctionBoundary = isFunctionBoundary;
			this.nextSlot = initialSlot;
		}
	}

	private final List<ScopeFrame> scopes;
	private final Map<String, List<Integer>> globalVariableSlots;
	private final Map<FunctionNameAndArity, List<Integer>> globalFunctionSlots;
	private final Set<String> globalVariables;
	private final Set<FunctionNameAndArity> globalFunctions;
	private final Set<Integer> globalVariableRootSlots;
	private final Set<Integer> globalFunctionRootSlots;

	public CompileContext() {
		this.scopes = new ArrayList<>();
		this.scopes.add(new ScopeFrame(true, 0));
		this.globalVariableSlots = new HashMap<>();
		this.globalFunctionSlots = new HashMap<>();
		this.globalVariables = new HashSet<>();
		this.globalFunctions = new HashSet<>();
		this.globalVariableRootSlots = new HashSet<>();
		this.globalFunctionRootSlots = new HashSet<>();
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

	/**
	 * Reserves the "static link" slot for the current (top) function scope: the fixed slot, within this
	 * function's own frame, that will hold this function's own Closure at runtime. Must be called on a
	 * function-boundary scope exactly once, immediately after its parameter slots are assigned and before
	 * its body is compiled, so that nested defs compiled while this scope is on top can reference the slot
	 * as a fixed, already-known constant (a capture crossing 2+ boundaries needs to know this number while
	 * the intermediate function's own frame layout is still being decided).
	 */
	public int reserveClosureSlot() {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		int slot = top.nextSlot++;
		top.closureSlot = slot;
		return slot;
	}

	/**
	 * The closure slot (see {@link #reserveClosureSlot()}) of the nearest enclosing function-boundary scope --
	 * i.e. where, in the frame that's live while code at the current compile position is executing, that
	 * function's own Closure will be found at runtime. Returns -1 if no enclosing scope ever reserved one
	 * (the implicit root scope), which is harmless: a top-level def's captures never need to read through it,
	 * since there is nothing beyond root to capture from.
	 */
	public int getCurrentFunctionClosureSlot() {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			ScopeFrame frame = scopes.get(i);
			if (frame.isFunctionBoundary)
				return frame.closureSlot;
		}
		return -1;
	}

	public void addLocalVariable(String name) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		top.variables.add(name);
		getOrAssignVariableSlotInTop(name);
	}

	public void addLocalFunction(String name, int arity) {
		if (scopes.isEmpty())
			pushFunctionScope();
		ScopeFrame top = scopes.get(scopes.size() - 1);
		FunctionNameAndArity key = FunctionNameAndArity.of(name, arity);
		top.functions.add(key);
		if (top == scopes.get(0) && globalFunctionRootSlots.contains(top.functionSlots.get(key))) {
			top.functionSlots.put(key, top.nextSlot++);
		} else {
			getOrAssignFunctionSlotInTop(key);
		}
	}

	private int getOrAssignVariableSlotInTop(String name) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		Integer slot = top.variableSlots.get(name);
		if (slot != null)
			return slot;
		int assigned = top.nextSlot++;
		top.variableSlots.put(name, assigned);
		return assigned;
	}

	private int getOrAssignFunctionSlotInTop(FunctionNameAndArity key) {
		ScopeFrame top = scopes.get(scopes.size() - 1);
		Integer slot = top.functionSlots.get(key);
		if (slot != null)
			return slot;
		int assigned = top.nextSlot++;
		top.functionSlots.put(key, assigned);
		return assigned;
	}

	public void addGlobalVariable(String symbolName, String bindingName) {
		ScopeFrame root = scopes.get(0);
		root.variables.add(symbolName);
		@Var Integer slot = root.variableSlots.get(symbolName);
		if (slot == null) {
			slot = root.nextSlot++;
			root.variableSlots.put(symbolName, slot);
		}
		globalVariables.add(bindingName);
		globalVariableRootSlots.add(slot);
		List<Integer> slots = globalVariableSlots.computeIfAbsent(bindingName, ignored -> new ArrayList<>());
		if (!slots.contains(slot))
			slots.add(slot);
	}

	public void addGlobalFunction(FunctionNameAndArity key) {
		ScopeFrame root = scopes.get(0);
		root.functions.add(key);
		@Var Integer slot = root.functionSlots.get(key);
		if (slot == null) {
			slot = root.nextSlot++;
			root.functionSlots.put(key, slot);
		}
		globalFunctions.add(key);
		globalFunctionRootSlots.add(slot);
		List<Integer> slots = globalFunctionSlots.computeIfAbsent(key, ignored -> new ArrayList<>());
		if (!slots.contains(slot))
			slots.add(slot);
	}

	public Map<String, List<Integer>> globalVariableSlots() {
		return globalVariableSlots;
	}

	public Map<FunctionNameAndArity, List<Integer>> globalFunctionSlots() {
		return globalFunctionSlots;
	}

	public Set<String> globalVariables() {
		return globalVariables;
	}

	public Set<FunctionNameAndArity> globalFunctions() {
		return globalFunctions;
	}

	public boolean isLocalVariable(String name) {
		return getVariableLocation(name) != null;
	}

	public boolean isLocalFunction(String name, int arity) {
		return getFunctionLocation(name, arity) != null;
	}

	public int getVariableSlot(String name) {
		SymbolLocation loc = getVariableLocation(name);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public int getFunctionSlot(String name, int arity) {
		SymbolLocation loc = getFunctionLocation(name, arity);
		if (loc != null)
			return loc.slot;
		return 0;
	}

	public @Nullable SymbolLocation getVariableLocation(String name) {
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		if (current.variables.contains(name)) {
			Integer slot = current.variableSlots.get(name);
			if (slot != null) {
				return currentDepth == 0 && globalVariableRootSlots.contains(slot)
						? SymbolLocation.global(slot) : SymbolLocation.local(slot);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			if (outer.variables.contains(name)) {
				Integer localSlot = outer.variableSlots.get(name);
				if (localSlot != null) {
					if (!crossedFunctionBoundary) {
						return i == 0 && globalVariableRootSlots.contains(localSlot)
								? SymbolLocation.global(localSlot) : SymbolLocation.local(localSlot);
					}
					boolean global = i == 0 && globalVariableRootSlots.contains(localSlot);
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
						if (closureSlot == null) {
							closureSlot = targetFrame.nextClosureSlot++;
							targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot, closureSlot));
							targetFrame.capturedVarSlots.put(name, closureSlot);
							if (global)
								targetFrame.capturedGlobalVariables.add(name);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return global ? SymbolLocation.capturedGlobal(targetSlot) : SymbolLocation.captured(targetSlot);
				}
			}
			Integer existingClosureSlot = outer.capturedVarSlots.get(name);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				boolean global = outer.capturedGlobalVariables.contains(name);
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedVarSlots.get(name);
					if (closureSlot == null) {
						closureSlot = targetFrame.nextClosureSlot++;
						targetFrame.capturedVariables.add(new ClosureSpec.CapturedVariableRef(isLocalInParent, targetSlot, closureSlot));
						targetFrame.capturedVarSlots.put(name, closureSlot);
						if (global)
							targetFrame.capturedGlobalVariables.add(name);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return global ? SymbolLocation.capturedGlobal(targetSlot) : SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}

	public @Nullable SymbolLocation getFunctionLocation(String name, int arity) {
		FunctionNameAndArity key = FunctionNameAndArity.of(name, arity);
		int currentDepth = scopes.size() - 1;
		ScopeFrame current = scopes.get(currentDepth);

		FunctionNameAndArity currentKey = resolveFunctionKey(current, key);
		if (currentKey != null) {
			Integer slot = current.functionSlots.get(currentKey);
			if (slot != null) {
				return currentDepth == 0 && globalFunctionRootSlots.contains(slot)
						? SymbolLocation.global(slot) : SymbolLocation.local(slot);
			}
		}

		@Var boolean crossedFunctionBoundary = false;
		for (int i = currentDepth - 1; i >= 0; i--) {
			ScopeFrame outer = scopes.get(i);
			if (scopes.get(i + 1).isFunctionBoundary) {
				crossedFunctionBoundary = true;
			}

			FunctionNameAndArity outerKey = resolveFunctionKey(outer, key);
			if (outerKey != null) {
				Integer localSlot = outer.functionSlots.get(outerKey);
				if (localSlot != null) {
					if (!crossedFunctionBoundary) {
						return i == 0 && globalFunctionRootSlots.contains(localSlot)
								? SymbolLocation.global(localSlot) : SymbolLocation.local(localSlot);
					}
					boolean global = i == 0 && globalFunctionRootSlots.contains(localSlot);
					@Var int targetSlot = localSlot;
					@Var boolean isLocalInParent = true;
					for (int k = i + 1; k <= currentDepth; k++) {
						ScopeFrame targetFrame = scopes.get(k);
						if (!targetFrame.isFunctionBoundary)
							continue;
						@Var Integer closureSlot = targetFrame.capturedFnSlots.get(outerKey);
						if (closureSlot == null) {
							closureSlot = targetFrame.nextClosureSlot++;
							targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot, closureSlot));
							targetFrame.capturedFnSlots.put(outerKey, closureSlot);
							if (global)
								targetFrame.capturedGlobalFunctions.add(outerKey);
						}
						targetSlot = closureSlot;
						isLocalInParent = false;
					}
					return global ? SymbolLocation.capturedGlobal(targetSlot) : SymbolLocation.captured(targetSlot);
				}
			}
			FunctionNameAndArity existingKey = outer.capturedFnSlots.containsKey(key) ? key : key.withArity(null);
			Integer existingClosureSlot = outer.capturedFnSlots.get(existingKey);
			if (existingClosureSlot != null && crossedFunctionBoundary) {
				boolean global = outer.capturedGlobalFunctions.contains(existingKey);
				@Var int targetSlot = existingClosureSlot;
				@Var boolean isLocalInParent = false;
				for (int k = i + 1; k <= currentDepth; k++) {
					ScopeFrame targetFrame = scopes.get(k);
					if (!targetFrame.isFunctionBoundary)
						continue;
					@Var Integer closureSlot = targetFrame.capturedFnSlots.get(existingKey);
					if (closureSlot == null) {
						closureSlot = targetFrame.nextClosureSlot++;
						targetFrame.capturedFunctions.add(new ClosureSpec.CapturedFunctionRef(isLocalInParent, targetSlot, closureSlot));
						targetFrame.capturedFnSlots.put(existingKey, closureSlot);
						if (global)
							targetFrame.capturedGlobalFunctions.add(existingKey);
					}
					targetSlot = closureSlot;
					isLocalInParent = false;
				}
				return global ? SymbolLocation.capturedGlobal(targetSlot) : SymbolLocation.captured(targetSlot);
			}
		}

		return null;
	}

	private static @Nullable FunctionNameAndArity resolveFunctionKey(ScopeFrame frame, FunctionNameAndArity key) {
		if (frame.functions.contains(key))
			return key;
		FunctionNameAndArity variadicKey = key.withArity(null);
		return frame.functions.contains(variadicKey) ? variadicKey : null;
	}
}
