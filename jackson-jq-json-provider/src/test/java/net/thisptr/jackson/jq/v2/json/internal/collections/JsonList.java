package net.thisptr.jackson.jq.v2.json.internal.collections;

import java.util.Arrays;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Lists of JSON nodes for tests.
 *
 * <p>A {@link JsonProvider} may use Java null as its JSON null node -- the fastjson2 provider does
 * -- so a list that can hold a node must tolerate null elements. {@code List.of} does not.
 */
public final class JsonList {
	private JsonList() {
	}

	// @SafeVarargs is the caller-side promise; the array is only read, never written or stored
	// under another type, so forwarding it to Arrays.asList cannot pollute the heap.
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> List<T> of(T... nodes) {
		return Arrays.asList(nodes);
	}
}
