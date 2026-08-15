package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;

public class CompileContext {
	private final Stack<Set<String>> localVariablesStack;
	private final Stack<Set<FunctionNameAndArity>> localFunctionsStack;

	private final java.util.Map<String, Integer> variableSlots;
	private int nextSlot;

	public CompileContext() {
		this.localVariablesStack = new Stack<>();
		this.localFunctionsStack = new Stack<>();
		this.variableSlots = new java.util.HashMap<>();
		this.nextSlot = 0;
	}

	private CompileContext(Stack<Set<String>> localVariablesStack, Stack<Set<FunctionNameAndArity>> localFunctionsStack, java.util.Map<String, Integer> variableSlots, int nextSlot) {
		this.localVariablesStack = new Stack<>();
		for (Set<String> set : localVariablesStack) {
			this.localVariablesStack.push(new HashSet<>(set));
		}
		this.localFunctionsStack = new Stack<>();
		for (Set<FunctionNameAndArity> set : localFunctionsStack) {
			this.localFunctionsStack.push(new HashSet<>(set));
		}
		this.variableSlots = new java.util.HashMap<>(variableSlots);
		this.nextSlot = nextSlot;
	}

	public CompileContext copy() {
		return new CompileContext(localVariablesStack, localFunctionsStack, variableSlots, nextSlot);
	}

	public void pushScope() {
		localVariablesStack.push(new HashSet<>());
		localFunctionsStack.push(new HashSet<>());
	}

	public void popScope() {
		if (!localVariablesStack.isEmpty())
			localVariablesStack.pop();
		if (!localFunctionsStack.isEmpty())
			localFunctionsStack.pop();
	}

	public void addLocalVariable(String name) {
		if (localVariablesStack.isEmpty())
			pushScope();
		localVariablesStack.peek().add(name);
		getOrAssignSlot(name);
	}

	public int getOrAssignSlot(String name) {
		Integer slot = variableSlots.get(name);
		if (slot != null)
			return slot;
		int assigned = nextSlot++;
		variableSlots.put(name, assigned);
		return assigned;
	}

	public int getSlot(String name) {
		Integer slot = variableSlots.get(name);
		return slot != null ? slot.intValue() : -1;
	}

	public int getSlotCount() {
		return nextSlot;
	}

	public boolean isLocalVariable(String name) {
		for (int i = localVariablesStack.size() - 1; i >= 0; i--) {
			if (localVariablesStack.get(i).contains(name))
				return true;
		}
		return false;
	}

	public void addLocalFunction(String name, int arity) {
		if (localFunctionsStack.isEmpty())
			pushScope();
		localFunctionsStack.peek().add(FunctionNameAndArity.of(name, arity));
	}

	public boolean isLocalFunction(String name, int arity) {
		FunctionNameAndArity key = FunctionNameAndArity.of(name, arity);
		for (int i = localFunctionsStack.size() - 1; i >= 0; i--) {
			if (localFunctionsStack.get(i).contains(key))
				return true;
		}
		return false;
	}
}
