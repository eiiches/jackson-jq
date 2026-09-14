package net.thisptr.jackson.jq.v2.json;

import java.util.NoSuchElementException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Holds either a value or nothing, keeping "absent" distinguishable from "present" without
 * spending Java {@code null} on the distinction.
 * <p>
 * This exists because {@link java.util.Optional} cannot: {@code Optional.of(null)} throws and
 * {@code Optional.ofNullable(null)} collapses to {@code Optional.empty()}, so an {@code Optional}
 * cannot express <i>present, and the value is Java {@code null}</i>. A {@link JsonProvider} whose
 * underlying library represents JSON {@code null} as Java {@code null} needs exactly that: for it,
 * {@code Maybe.of(null)} is a present JSON {@code null}, which is a different answer from
 * {@link #absent()}.
 * <p>
 * Consequently {@link #get()} may return Java {@code null}, and is deliberately not annotated
 * {@code @Nullable}: a {@code null} it returns is a value, never a signal that nothing is there.
 * Ask {@link #isAbsent()} for that.
 *
 * @param <T> the type of the held value
 */
public final class Maybe<T extends @Nullable Object> {
	private static final Maybe<?> ABSENT = new Maybe<Object>(false, null);

	private final boolean present;

	/**
	 * The held value, as a plain {@code Object} so that a Java {@code null} payload -- a present
	 * JSON {@code null} -- can be stored without NullAway reading it as an absence.
	 */
	private final @Nullable Object value;

	private Maybe(boolean present, @Nullable Object value) {
		this.present = present;
		this.value = value;
	}

	// The only values ever stored are those handed to of(T), so the cast back to T is sound.
	// NullAway cannot express the point of this class -- that T may stand for a type whose Java
	// null is a value rather than an absence -- so it reads the payload as non-null here.
	@SuppressWarnings({ "unchecked", "NullAway" })
	private T value() {
		return (T) value;
	}

	/**
	 * Returns the instance holding nothing.
	 *
	 * @param <T> the type of the value that is not there
	 * @return the absent instance
	 */
	@SuppressWarnings("unchecked")
	public static <T extends @Nullable Object> Maybe<T> absent() {
		return (Maybe<T>) ABSENT;
	}

	/**
	 * Returns an instance holding the given value.
	 * <p>
	 * The value may be Java {@code null}, which makes this a present {@code null} rather than
	 * {@link #absent()} -- see the class documentation.
	 *
	 * @param <T> the type of the held value
	 * @param value the value to hold
	 * @return an instance holding {@code value}
	 */
	public static <T extends @Nullable Object> Maybe<T> of(T value) {
		return new Maybe<>(true, value);
	}

	/**
	 * Returns whether a value is held.
	 *
	 * @return {@code true} if a value is held, even a Java {@code null} one
	 */
	public boolean isPresent() {
		return present;
	}

	/**
	 * Returns whether nothing is held.
	 *
	 * @return {@code true} if no value is held
	 */
	public boolean isAbsent() {
		return !present;
	}

	/**
	 * Returns the held value.
	 * <p>
	 * The result is Java {@code null} if the held value is; that is a value, not an absence.
	 *
	 * @return the held value
	 * @throws NoSuchElementException if nothing is held
	 */
	public T get() {
		if (!present)
			throw new NoSuchElementException("No value present");
		return value();
	}

	/**
	 * Returns the held value, or {@code other} if nothing is held.
	 *
	 * @param other the value to return when nothing is held
	 * @return the held value, or {@code other}
	 */
	public T orElse(T other) {
		return present ? value() : other;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Maybe))
			return false;
		Maybe<?> other = (Maybe<?>) o;
		return present == other.present && Objects.equals(value, other.value);
	}

	@Override
	public int hashCode() {
		return present ? 31 + Objects.hashCode(value) : 0;
	}

	@Override
	public String toString() {
		return present ? "Maybe.of(" + value + ")" : "Maybe.absent()";
	}
}
