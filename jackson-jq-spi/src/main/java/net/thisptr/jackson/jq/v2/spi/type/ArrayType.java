package net.thisptr.jackson.jq.v2.spi.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

/**
 * An array type with known leading elements and a type for the elements past them. A
 * {@link NeverType} additional element type makes the array closed, bounding its length by the
 * number of known elements.
 * <p>
 * A known element whose type includes {@link UndefinedType} may be absent; one whose type does not
 * is present in every array the type accepts. Optional elements must be trailing: a required element
 * cannot follow an optional element. An element past the known ones is never known to be
 * present. This mirrors {@link ObjectType}, where a declared field is optional exactly when its type
 * includes {@link UndefinedType}.
 */
public final class ArrayType implements Type {
	private final List<Type> knownElements;
	private final Type additionalElementType;
	private final Type elementType;

	/**
	 * Creates an open {@code ArrayType} accepting arrays whose every element has the given type.
	 *
	 * @param elementType the required element type
	 * @return a new {@code ArrayType}
	 */
	public static ArrayType of(Type elementType) {
		return of(List.of(), Objects.requireNonNull(elementType, "elementType"));
	}

	/**
	 * Creates an open {@code ArrayType} whose leading positions have the given types and whose
	 * remaining elements have {@code additionalElementType}.
	 *
	 * @param knownElements the types of the leading positions
	 * @param additionalElementType the type of the elements past them
	 * @return a new {@code ArrayType}
	 */
	public static ArrayType of(List<? extends Type> knownElements, Type additionalElementType) {
		Objects.requireNonNull(knownElements, "knownElements");
		Objects.requireNonNull(additionalElementType, "additionalElementType");
		return new ArrayType(normalize(knownElements, additionalElementType), additionalElementType);
	}

	/**
	 * Creates a closed {@code ArrayType} whose leading positions have the given types.
	 * <p>
	 * A position is optional when its type includes {@link UndefinedType}, and present otherwise.
	 * Since the array is closed, it has no elements past the ones given.
	 *
	 * @param knownElements the types of the leading positions
	 * @return a new {@code ArrayType}
	 */
	public static ArrayType of(List<? extends Type> knownElements) {
		return of(knownElements, NeverType.getInstance());
	}

	/**
	 * Canonicalizes the known elements so that two types spelling the same set of arrays are equal.
	 * <p>
	 * A required element cannot follow an optional element, because omitting an earlier element would
	 * shift subsequent elements to earlier indices rather than leaving a hole. A trailing element that
	 * says no more than the additional element type already says is redundant, and is dropped.
	 */
	private static List<Type> normalize(List<? extends Type> knownElements, Type additionalElementType) {
		List<Type> result = new ArrayList<>(knownElements.size());
		@Var
		boolean optional = false;
		for (Type knownElement : knownElements) {
			Objects.requireNonNull(knownElement, "knownElements must not contain null elements");
			boolean isOptional = includesUndefined(knownElement);
			if (optional && !isOptional)
				throw new IllegalArgumentException("A required element cannot follow an optional element: " + knownElement);
			optional = isOptional;
			result.add(knownElement);
		}
		// A closed array reduces this to UNDEFINED on its own, since NEVER is the union's identity.
		Type redundant = UnionType.of(additionalElementType, UndefinedType.getInstance());
		while (!result.isEmpty() && result.get(result.size() - 1).equals(redundant))
			result.remove(result.size() - 1);
		return result;
	}

	private static boolean includesUndefined(Type type) {
		if (type == UndefinedType.getInstance())
			return true;
		return type instanceof UnionType union && union.alternatives().contains(UndefinedType.getInstance());
	}

	private static Type withoutUndefined(Type type) {
		if (type == UndefinedType.getInstance())
			return NeverType.getInstance();
		if (!(type instanceof UnionType union))
			return type;
		List<Type> alternatives = new ArrayList<>(union.alternatives());
		return alternatives.remove(UndefinedType.getInstance()) ? UnionType.of(alternatives) : type;
	}

	private ArrayType(List<Type> knownElements, Type additionalElementType) {
		this.knownElements = Collections.unmodifiableList(knownElements);
		this.additionalElementType = additionalElementType;
		if (knownElements.isEmpty()) {
			this.elementType = additionalElementType;
		} else {
			List<Type> elementTypes = new ArrayList<>(knownElements.size() + 1);
			for (Type knownElement : knownElements)
				elementTypes.add(withoutUndefined(knownElement));
			elementTypes.add(additionalElementType);
			this.elementType = UnionType.of(elementTypes);
		}
	}

	/**
	 * Returns the types of the leading positions, in order.
	 *
	 * @return the immutable known element types
	 */
	public List<Type> knownElements() {
		return knownElements;
	}

	/**
	 * Returns the type of the elements past the known ones, or {@link NeverType} for a closed array.
	 *
	 * @return the additional element type
	 */
	public Type additionalElementType() {
		return additionalElementType;
	}

	/**
	 * Returns whether the array has no elements past its known ones, which bounds its length.
	 *
	 * @return whether the array is closed
	 */
	public boolean isClosed() {
		return additionalElementType == NeverType.getInstance();
	}

	/**
	 * Returns the type accepted for an array element at any position, which is the union of the known
	 * element types and the additional element type. Whether a position may be absent is a claim about
	 * length rather than about an element, so {@link UndefinedType} is not part of the answer.
	 *
	 * @return the element type
	 */
	public Type elementType() {
		return elementType;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return obj instanceof ArrayType other && knownElements.equals(other.knownElements)
				&& additionalElementType.equals(other.additionalElementType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(knownElements, additionalElementType);
	}

	@Override
	public String toString() {
		return TypeNotation.print(this);
	}
}
