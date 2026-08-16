package net.thisptr.jackson.jq.v2.core.internal.compile;

public class SymbolLocation {
	public final boolean isLocal;
	public final boolean isGlobal;
	public final int slot;

	private SymbolLocation(boolean isLocal, boolean isGlobal, int slot) {
		this.isLocal = isLocal;
		this.isGlobal = isGlobal;
		this.slot = slot;
	}

	public static SymbolLocation local(int slot) {
		return new SymbolLocation(true, false, slot);
	}

	public static SymbolLocation global(int slot) {
		return new SymbolLocation(true, true, slot);
	}

	public static SymbolLocation captured(int closureSlot) {
		return new SymbolLocation(false, false, closureSlot);
	}

	public static SymbolLocation capturedGlobal(int closureSlot) {
		return new SymbolLocation(false, true, closureSlot);
	}
}
