package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CompileContext {
	private final Stack<Set<String>> localVariablesStack = new Stack<>();

	public void pushScope() {
		localVariablesStack.push(new HashSet<>());
	}

	public void popScope() {
		if (!localVariablesStack.isEmpty())
			localVariablesStack.pop();
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
}
