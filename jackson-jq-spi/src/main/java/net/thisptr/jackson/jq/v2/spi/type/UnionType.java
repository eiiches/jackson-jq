package net.thisptr.jackson.jq.v2.spi.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

/**
 * A normalized union of two or more distinct alternatives.
 */
public final class UnionType implements Type {
	private static final Comparator<Type> CANONICAL_ORDER = Comparator.comparing(Type::toString);

	/**
	 * How many known strings a union names one by one. A discrimination anyone writes by hand sits far
	 * inside this; past it the alternatives are only expensive to carry around, and the plain string
	 * type says the one thing left worth saying.
	 */
	private static final int MAX_STRING_LITERALS = 16;

	private final List<Type> alternatives;

	public static Type of(Type... alternatives) {
		return of(Arrays.asList(alternatives));
	}

	/**
	 * Returns the normalized union of the given alternatives.
	 * <p>
	 * Nested unions are flattened, duplicates are removed, and the survivors are put in canonical
	 * order. {@link AnyType} absorbs every other alternative except {@link UndefinedType}, and a
	 * union that normalizes to a single alternative is that alternative.
	 *
	 * @param alternatives union alternatives
	 * @return a normalized union, its only alternative, or {@link AnyType}
	 */
	public static Type of(Iterable<? extends Type> alternatives) {
		List<Type> flattened = new ArrayList<>();
		for (Type alternative : alternatives) {
			Objects.requireNonNull(alternative, "alternatives must not contain null elements");
			if (alternative instanceof UnionType union)
				flattened.addAll(union.alternatives);
			else
				flattened.add(alternative);
		}
		flattened.removeIf(alternative -> alternative == NeverType.getInstance());
		if (flattened.isEmpty())
			return NeverType.getInstance();
		if (flattened.contains(AnyType.getInstance())) {
			if (!flattened.contains(UndefinedType.getInstance()))
				return AnyType.getInstance();
			return new UnionType(List.of(AnyType.getInstance(), UndefinedType.getInstance()));
		}

		flattened.sort(CANONICAL_ORDER);
		List<Type> unique = new ArrayList<>(flattened.size());
		for (Type alternative : flattened) {
			if (unique.isEmpty() || !unique.get(unique.size() - 1).equals(alternative))
				unique.add(alternative);
		}
		@Var
		boolean collapsed = collapseNumbers(unique);
		collapsed |= collapseLiterals(unique);
		if (collapsed)
			unique.sort(CANONICAL_ORDER);
		if (unique.size() == 1)
			return unique.get(0);
		return new UnionType(unique);
	}

	/**
	 * Replaces several distinct number alternatives with the plain number type, answering whether it
	 * did. A number kind is a hint, so a union of several of them says no more than {@link NumericType}
	 * does on its own.
	 */
	private static boolean collapseNumbers(List<Type> alternatives) {
		@Var
		int numbers = 0;
		for (Type alternative : alternatives) {
			if (alternative instanceof NumericType)
				numbers++;
		}
		if (numbers < 2)
			return false;
		alternatives.removeIf(NumericType.class::isInstance);
		alternatives.add(NumericType.getInstance());
		return true;
	}

	/**
	 * Replaces the known strings and booleans a union names with the type they are all instances of,
	 * answering whether it did. A value is dropped once the type it is an instance of is there anyway,
	 * {@code true | false} is no more than a boolean, and too many strings are no more than a string.
	 */
	private static boolean collapseLiterals(List<Type> alternatives) {
		@Var
		boolean collapsed = false;
		if (alternatives.contains(StringType.getInstance()))
			collapsed = alternatives.removeIf(alternative -> alternative instanceof StringType string && string.value() != null);
		else if (alternatives.stream().filter(StringType.class::isInstance).count() > MAX_STRING_LITERALS) {
			alternatives.removeIf(StringType.class::isInstance);
			alternatives.add(StringType.getInstance());
			collapsed = true;
		}
		if (alternatives.contains(BooleanType.getInstance()))
			collapsed |= alternatives.removeIf(alternative -> alternative instanceof BooleanType bool && bool.value() != null);
		else if (alternatives.contains(BooleanType.of(true)) && alternatives.contains(BooleanType.of(false))) {
			alternatives.removeIf(BooleanType.class::isInstance);
			alternatives.add(BooleanType.getInstance());
			collapsed = true;
		}
		return collapsed;
	}

	private UnionType(List<Type> alternatives) {
		this.alternatives = List.copyOf(alternatives);
	}

	/**
	 * Returns the alternatives in canonical order.
	 *
	 * @return the immutable alternatives
	 */
	public List<Type> alternatives() {
		return alternatives;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof UnionType other && alternatives.equals(other.alternatives);
	}

	@Override
	public int hashCode() {
		return alternatives.hashCode();
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
