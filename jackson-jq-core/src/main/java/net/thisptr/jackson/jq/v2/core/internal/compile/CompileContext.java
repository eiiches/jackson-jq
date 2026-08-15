package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import net.thisptr.jackson.jq.v2.spi.FunctionNameAndArity;

public class CompileContext {
	private final Stack<Set<String>> localVariablesStack;
	private final Stack<Set<FunctionNameAndArity>> localFunctionsStack;

	public CompileContext() {
		this.localVariablesStack = new Stack<>();
		this.localFunctionsStack = new Stack<>();
	}

	private CompileContext(Stack<Set<String>> localVariablesStack, Stack<Set<FunctionNameAndArity>> localFunctionsStack) {
		this.localVariablesStack = new Stack<>();
		for (Set<String> set : localVariablesStack) {
			this.localVariablesStack.push(new HashSet<>(set));
		}
		this.localFunctionsStack = new Stack<>();
		for (Set<FunctionNameAndArity> set : localFunctionsStack) {
			this.localFunctionsStack.push(new HashSet<>(set));
		}
	}

	public CompileContext copy() {
		return new CompileContext(localVariablesStack, localFunctionsStack);
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
