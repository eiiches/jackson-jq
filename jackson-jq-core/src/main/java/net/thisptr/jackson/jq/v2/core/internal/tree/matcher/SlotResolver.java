package net.thisptr.jackson.jq.v2.core.internal.tree.matcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Assigns frame slots to the variables of one destructuring pattern, during
 * {@link PatternMatcher#resolveSlots}.
 * <p>
 * A resolver is single-use and order-sensitive: it must be walked in the order the matchers traverse
 * at runtime, because that is what lets it decide duplicate-variable precedence statically.
 */
public final class SlotResolver {
	private final Map<String, Integer> slots;
	private final Set<String> claimed = new HashSet<>();

	public SlotResolver(Map<String, Integer> slots) {
		this.slots = slots;
	}

	/**
	 * The slot the occurrence of {@code name} at the current traversal position must write, or -1 if
	 * an earlier position already binds it. jq keeps the first occurrence of a duplicated pattern
	 * variable -- {@code . as [$x, $x] | $x} is the second array element because {@link
	 * net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers.ArrayMatcher} traverses back to
	 * front -- so every later occurrence is a dead write and is suppressed here rather than at
	 * runtime. Only the write is suppressed: the occurrence still traverses, and still branches if
	 * its key expression produces several keys.
	 */
	public int claim(String name) {
		Integer slot = slots.get(name);
		if (slot == null)
			throw new IllegalStateException("No slot allocated for pattern variable $" + name);
		if (!claimed.add(name))
			return -1;
		return slot;
	}
}
