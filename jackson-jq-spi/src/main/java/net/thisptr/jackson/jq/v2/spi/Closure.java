package net.thisptr.jackson.jq.v2.spi;

import org.jspecify.annotations.Nullable;

public class Closure {
	private final Object[] slots;

	public Closure(int size) {
		this.slots = new Object[size];
	}

	public @Nullable Object getRawValue(int slot) {
		if (slot < 0 || slot >= slots.length)
			return null;
		return slots[slot];
	}

	public void setRawValue(int slot, @Nullable Object value) {
		if (slot >= 0 && slot < slots.length) {
			slots[slot] = value;
		}
	}
}
