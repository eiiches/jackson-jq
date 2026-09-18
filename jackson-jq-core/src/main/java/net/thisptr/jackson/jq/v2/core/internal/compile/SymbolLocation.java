package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Where a local/captured symbol (a {@code def} parameter or {@code as}/{@code reduce}/{@code foreach}
 * binding) lives -- {@code local} (a direct slot in the current {@code StackFrame}) or {@code captured} (a
 * slot in a {@code Closure}, reached across one or more {@code def} boundaries). Declared/defined globals
 * never produce a {@code SymbolLocation}: they're resolved directly against the {@code Environment} once
 * the scope-stack walk that produces these finds nothing (see {@code Compiler#compileFunctionCall}/
 * {@code compileGlobalVariableAccess}).
 */
public class SymbolLocation {
	public final boolean isLocal;
	public final int slot;

	/**
	 * Precomputed {@code dependsOn*()} facts about the {@code def} this location resolves to -- {@code null}
	 * for variable locations (which never carry one) and for function locations whose def hasn't finished
	 * compiling yet (self-recursive/forward/mutually-recursive calls). See {@link FunctionDependsOnInfo}.
	 */
	public final @Nullable FunctionDependsOnInfo dependsOnInfo;
	public final @Nullable BoundArgumentInfo boundArgumentInfo;

	/**
	 * The declared parameter names of the {@code def} this location resolves to, {@code $}-prefixed for a
	 * value parameter -- {@code null} for a variable location, and for a function reached by a route that does
	 * not identify the definition (a re-capture of something captured further out). A tail call needs them to
	 * tell which parameters take a value and which take a filter; nothing else reads them.
	 */
	public final @Nullable List<String> parameterNames;

	private SymbolLocation(boolean isLocal, int slot, @Nullable FunctionDependsOnInfo dependsOnInfo, @Nullable BoundArgumentInfo boundArgumentInfo, @Nullable List<String> parameterNames) {
		this.isLocal = isLocal;
		this.slot = slot;
		this.dependsOnInfo = dependsOnInfo;
		this.boundArgumentInfo = boundArgumentInfo;
		this.parameterNames = parameterNames;
	}

	/**
	 * Returns {@code location} with the resolved definition's parameter names attached.
	 *
	 * @param location the location to copy
	 * @param parameterNames the declared parameter names, or {@code null} if unknown
	 * @return {@code location} itself when there is nothing to attach, otherwise a copy carrying them
	 */
	public static SymbolLocation withParameterNames(SymbolLocation location, @Nullable List<String> parameterNames) {
		if (parameterNames == null)
			return location;
		return new SymbolLocation(location.isLocal, location.slot, location.dependsOnInfo, location.boundArgumentInfo, parameterNames);
	}

	public static SymbolLocation local(int slot) {
		return new SymbolLocation(true, slot, null, null, null);
	}

	public static SymbolLocation local(int slot, @Nullable FunctionDependsOnInfo dependsOnInfo) {
		return new SymbolLocation(true, slot, dependsOnInfo, null, null);
	}

	public static SymbolLocation localBoundArgument(int slot, BoundArgumentInfo boundArgumentInfo) {
		return new SymbolLocation(true, slot, null, boundArgumentInfo, null);
	}

	public static SymbolLocation captured(int closureSlot) {
		return new SymbolLocation(false, closureSlot, null, null, null);
	}

	public static SymbolLocation captured(int closureSlot, @Nullable FunctionDependsOnInfo dependsOnInfo) {
		return new SymbolLocation(false, closureSlot, dependsOnInfo, null, null);
	}

	public static SymbolLocation capturedBoundArgument(int closureSlot, BoundArgumentInfo boundArgumentInfo) {
		return new SymbolLocation(false, closureSlot, null, boundArgumentInfo, null);
	}
}
