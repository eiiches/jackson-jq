package net.thisptr.jackson.jq.v2.core.internal.compile;

public class SymbolLocation {
	public final boolean isLocal;
	public final int slot;

	private SymbolLocation(boolean isLocal, int slot) {
		this.isLocal = isLocal;
		this.slot = slot;
	}

	public static SymbolLocation local(int slot) {
		return new SymbolLocation(true, slot);
	}

	public static SymbolLocation captured(int closureSlot) {
		return new SymbolLocation(false, closureSlot);
	}
}
