package net.thisptr.jackson.jq.v2.core.internal.utils;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.path.PathAndValue;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Encodes and decodes the bound value of a variable held in a stack frame slot.
 * <p>
 * A slot is an untyped {@code Object} that may hold a bound value, a function, a filter, or nothing
 * at all, and a bare Java {@code null} in it means <em>nothing</em>. A provider whose underlying
 * library represents JSON {@code null} as Java {@code null} therefore cannot have its {@code null}
 * stored as itself -- {@code null as $x | $x} would read back as an unset variable -- so such a
 * value goes in as {@link #JSON_NULL} instead. Everything writing a bound value into a slot goes
 * through {@link #toSlot}, and everything reading one back through {@link #asPathAndValue}.
 */
public final class StackFrameValues {
	/**
	 * Stands in, inside a slot, for a JSON {@code null} that the provider represents as Java
	 * {@code null}.
	 */
	private static final Object JSON_NULL = new Object();

	private StackFrameValues() {
	}

	/**
	 * Encodes a bound value for storage in a stack frame slot.
	 *
	 * @param value the bound value; Java {@code null} if the provider represents JSON {@code null}
	 * that way
	 * @return the object to store in the slot, never Java {@code null}
	 */
	public static <JsonNode> Object toSlot(JsonNode value) {
		return value == null ? JSON_NULL : value;
	}

	/**
	 * Decodes the contents of a stack frame slot.
	 *
	 * @param raw the slot contents
	 * @return the bound value with its path, or {@code null} if the slot holds no bound value
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable <JsonNode> PathAndValue<JsonNode> asPathAndValue(@Nullable Object raw) {
		if (raw instanceof PathAndValue) {
			return (PathAndValue<JsonNode>) raw;
		} else if (raw == JSON_NULL) {
			return new PathAndValue<>(UntrackedPath.getInstance(), StackFrameValues.<JsonNode>jsonNull());
		} else if (raw != null && !(raw instanceof Function) && !(raw instanceof Expression)) {
			return new PathAndValue<>(UntrackedPath.getInstance(), (JsonNode) raw);
		}
		return null;
	}

	// Reconstructs the Java null a provider uses for JSON null. NullAway cannot express that a
	// JsonNode-typed value may legitimately be Java null, so the null has to be laundered here.
	@SuppressWarnings("NullAway")
	private static <JsonNode> JsonNode jsonNull() {
		return null;
	}
}
