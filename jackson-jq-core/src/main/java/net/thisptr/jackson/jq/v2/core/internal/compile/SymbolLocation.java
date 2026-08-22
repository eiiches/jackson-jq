package net.thisptr.jackson.jq.v2.core.internal.compile;

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

	private SymbolLocation(boolean isLocal, int slot, @Nullable FunctionDependsOnInfo dependsOnInfo, @Nullable BoundArgumentInfo boundArgumentInfo) {
		this.isLocal = isLocal;
		this.slot = slot;
		this.dependsOnInfo = dependsOnInfo;
		this.boundArgumentInfo = boundArgumentInfo;
	}

	public static SymbolLocation local(int slot) {
		return new SymbolLocation(true, slot, null, null);
	}

	public static SymbolLocation local(int slot, @Nullable FunctionDependsOnInfo dependsOnInfo) {
		return new SymbolLocation(true, slot, dependsOnInfo, null);
	}

	public static SymbolLocation localBoundArgument(int slot, BoundArgumentInfo boundArgumentInfo) {
		return new SymbolLocation(true, slot, null, boundArgumentInfo);
	}

	public static SymbolLocation captured(int closureSlot) {
		return new SymbolLocation(false, closureSlot, null, null);
	}

	public static SymbolLocation captured(int closureSlot, @Nullable FunctionDependsOnInfo dependsOnInfo) {
		return new SymbolLocation(false, closureSlot, dependsOnInfo, null);
	}

	public static SymbolLocation capturedBoundArgument(int closureSlot, BoundArgumentInfo boundArgumentInfo) {
		return new SymbolLocation(false, closureSlot, null, boundArgumentInfo);
	}
}
