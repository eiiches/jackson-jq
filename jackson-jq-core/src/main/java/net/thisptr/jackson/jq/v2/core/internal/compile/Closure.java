package net.thisptr.jackson.jq.v2.core.internal.compile;

import org.jspecify.annotations.Nullable;

public class Closure {
	private final Object[] slots;

	public Closure(int size) {
		this.slots = new Object[size];
	}

	public int size() {
		return slots.length;
	}

	public @Nullable Object get(int slot) {
		if (slot < 0 || slot >= slots.length)
			throw new IndexOutOfBoundsException("slot " + slot + " out of bounds for closure size " + slots.length);
		return slots[slot];
	}

	public void set(int slot, @Nullable Object value) {
		if (slot < 0 || slot >= slots.length)
			throw new IndexOutOfBoundsException("slot " + slot + " out of bounds for closure size " + slots.length);
		slots[slot] = value;
	}
}
