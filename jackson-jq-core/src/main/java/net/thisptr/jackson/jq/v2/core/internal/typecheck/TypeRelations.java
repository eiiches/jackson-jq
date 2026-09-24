package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * The rules relating jq's operators and accessors to types, one rule per runtime operation.
 * <p>
 * Each rule mirrors what {@code BinaryOperations} and the expression tree actually do -- including where
 * this engine differs from upstream jq, as with {@code "x" * 0}, which {@code BinaryOperations.repeat}
 * answers {@code null} in every version. A rule raises {@link Problem} exactly when the operation fails
 * for every value the operand types admit; {@link TypeCheck} turns that into a diagnostic.
 */
final class TypeRelations {
	private TypeRelations() {
	}

	static List<Type> alternatives(Type type) {
		return type instanceof UnionType union ? union.alternatives() : List.of(type);
	}

	static Type field(Type target, String field) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : alternatives(target)) {
			if (alternative instanceof AnyType) {
				outputs.add(AnyType.getInstance());
			} else if (alternative instanceof NullType) {
				outputs.add(NullType.getInstance());
			} else if (alternative instanceof ObjectType object) {
				Type declared = object.fields().get(field);
				outputs.add(declared != null ? absentAsNull(declared)
						: object.isClosed() ? NullType.getInstance() : absentAsNull(UnionType.of(object.additionalFieldType(), UndefinedType.getInstance())));
			} else {
				throw new Problem("Cannot index " + alternative + " with a string");
			}
		}
		return UnionType.of(outputs);
	}

	/**
	 * The type at {@code index}, {@link UndefinedType} included when the array may not reach it. A known
	 * element says whether its position is present; a position past them never does.
	 */
	static Type elementAt(ArrayType array, int index) {
		if (index < array.knownElements().size())
			return array.knownElements().get(index);
		return array.isClosed() ? UndefinedType.getInstance() : UnionType.of(array.additionalElementType(), UndefinedType.getInstance());
	}

	/**
	 * The length of an array whose length is known exactly -- closed, and with every known element
	 * present. Only such an array can say where a concatenation puts the elements that follow it, or what
	 * a negative index counts back from.
	 */
	static OptionalInt exactLength(ArrayType array) {
		if (!array.isClosed())
			return OptionalInt.empty();
		for (Type knownElement : array.knownElements()) {
			if (alternatives(knownElement).contains(UndefinedType.getInstance()))
				return OptionalInt.empty();
		}
		return OptionalInt.of(array.knownElements().size());
	}

	static Type absentAsNull(Type type) {
		List<Type> result = new ArrayList<>();
		for (Type alternative : alternatives(type))
			result.add(alternative == UndefinedType.getInstance() ? NullType.getInstance() : alternative);
		return UnionType.of(result);
	}

	static Type plus(Type left, Type right) {
		return pairs(left, right, (l, r) -> {
			if (l instanceof NullType)
				return r;
			if (r instanceof NullType)
				return l;
			if (l instanceof AnyType || r instanceof AnyType)
				return AnyType.getInstance();
			if (l instanceof NumericType && r instanceof NumericType)
				return NumericType.getInstance();
			if (l instanceof StringType && r instanceof StringType)
				return StringType.getInstance();
			if (l instanceof ArrayType la && r instanceof ArrayType ra)
				return concatenate(la, ra);
			if (l instanceof ObjectType lo && r instanceof ObjectType ro)
				return mergeObjects(lo, ro, false);
			throw new Problem(l + " and " + r + " cannot be added");
		});
	}

	/**
	 * Concatenation puts the right array's elements at a fixed offset from its own, so it keeps their
	 * positions -- but only when the left array's length is known exactly, since that offset is the length.
	 */
	private static Type concatenate(ArrayType left, ArrayType right) {
		if (exactLength(left).isEmpty())
			return ArrayType.of(UnionType.of(left.elementType(), right.elementType()));
		List<Type> knownElements = new ArrayList<>(left.knownElements());
		knownElements.addAll(right.knownElements());
		return ArrayType.of(knownElements, right.additionalElementType());
	}

	static Type arithmetic(Type left, Type right, String operation) {
		return pairs(left, right, (l, r) -> {
			if (l instanceof AnyType || r instanceof AnyType)
				return AnyType.getInstance();
			if (l instanceof NumericType && r instanceof NumericType)
				return NumericType.getInstance();
			throw new Problem(l + " and " + r + " cannot be " + operation);
		});
	}

	static Type minus(Type left, Type right) {
		return pairs(left, right, (l, r) -> {
			if (l instanceof AnyType || r instanceof AnyType)
				return AnyType.getInstance();
			if (l instanceof NumericType && r instanceof NumericType)
				return NumericType.getInstance();
			// Subtraction removes elements, so nothing is left at the position it was written at.
			if (l instanceof ArrayType array && r instanceof ArrayType)
				return ArrayType.of(array.elementType());
			throw new Problem(l + " and " + r + " cannot be subtracted");
		});
	}

	static Type multiply(Type left, Type right) {
		return pairs(left, right, (l, r) -> {
			if (l instanceof AnyType || r instanceof AnyType)
				return AnyType.getInstance();
			if (l instanceof NumericType && r instanceof NumericType)
				return NumericType.getInstance();
			if ((l instanceof StringType && r instanceof NumericType)
					|| (l instanceof NumericType && r instanceof StringType))
				return UnionType.of(StringType.getInstance(), NullType.getInstance());
			if (l instanceof ObjectType lo && r instanceof ObjectType ro)
				return mergeObjects(lo, ro, true);
			throw new Problem(l + " and " + r + " cannot be multiplied");
		});
	}

	static Type divide(Type left, Type right) {
		return pairs(left, right, (l, r) -> {
			if (l instanceof AnyType || r instanceof AnyType)
				return AnyType.getInstance();
			if (l instanceof NumericType && r instanceof NumericType)
				return NumericType.getInstance();
			if (l instanceof StringType && r instanceof StringType)
				return ArrayType.of(StringType.getInstance());
			throw new Problem(l + " and " + r + " cannot be divided");
		});
	}

	static Type modulo(Type left, Type right) {
		return arithmetic(left, right, "divided (remainder)");
	}

	private static Type mergeObjects(ObjectType left, ObjectType right, boolean recursive) {
		Map<String, Type> fields = new TreeMap<>();
		fields.putAll(left.fields());
		for (Map.Entry<String, Type> field : right.fields().entrySet()) {
			Type leftType = left.fields().get(field.getKey());
			if (recursive && leftType instanceof ObjectType leftObject && field.getValue() instanceof ObjectType rightObject)
				fields.put(field.getKey(), mergeObjects(leftObject, rightObject, true));
			else
				fields.put(field.getKey(), field.getValue());
		}
		return ObjectType.of(fields, UnionType.of(left.additionalFieldType(), right.additionalFieldType()));
	}

	/**
	 * The values {@code .[]} emits. An object yields its field types rather than {@code ANY}: which field
	 * is reached is not known, but the set of types it could hold is.
	 */
	static Type iterate(Type input) {
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : alternatives(input)) {
			if (alternative instanceof AnyType)
				outputs.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType array)
				outputs.add(array.elementType());
			else if (alternative instanceof ObjectType object)
				outputs.add(objectMemberValue(object));
			else
				throw new Problem("Cannot iterate over " + alternative);
		}
		return UnionType.of(outputs);
	}

	/**
	 * The type of a member reached by a key that is not statically known. {@link NeverType} for a closed
	 * object with no fields, because there is no member to reach.
	 */
	static Type objectMemberValue(ObjectType object) {
		List<Type> values = new ArrayList<>(object.fields().values());
		values.add(object.additionalFieldType());
		return UnionType.of(values);
	}

	/**
	 * Like {@link #objectMemberValue}, for a read that answers null for an absent key rather than emitting
	 * nothing -- {@code .[$k]} and an object destructuring pattern, but not {@code .[]}.
	 */
	static Type objectMemberOrNull(ObjectType object) {
		return UnionType.of(objectMemberValue(object), NullType.getInstance());
	}

	static Type slice(Type input) {
		if (input instanceof AnyType)
			return UnionType.of(ArrayType.of(AnyType.getInstance()), StringType.getInstance(), NullType.getInstance());
		List<Type> outputs = new ArrayList<>();
		for (Type alternative : alternatives(input)) {
			// A slice starts wherever its bounds say, so it keeps the element types and no position.
			if (alternative instanceof ArrayType array)
				outputs.add(ArrayType.of(array.elementType()));
			else if (alternative instanceof StringType || alternative instanceof NullType
					|| alternative instanceof AnyType)
				outputs.add(alternative);
			else
				throw new Problem("Cannot index " + alternative + " with a slice");
		}
		return UnionType.of(outputs);
	}

	static Type negate(Type input) {
		for (Type alternative : alternatives(input)) {
			if (!(alternative instanceof AnyType || alternative instanceof NumericType))
				throw new Problem(alternative + " cannot be negated");
		}
		return input instanceof AnyType ? AnyType.getInstance() : NumericType.getInstance();
	}

	/**
	 * What an array destructuring pattern binds at {@code index}.
	 * <p>
	 * A position the value does not reach is null at runtime, and an array type says which positions those
	 * are: a known element is present unless its type admits {@link UndefinedType}, and a position past
	 * the known ones is never known to be reached. Alternatives no array pattern can match are an error,
	 * as they are at runtime.
	 */
	static Type arrayPatternElement(Type input, int index) {
		List<Type> elements = new ArrayList<>();
		for (Type alternative : alternatives(input)) {
			if (alternative instanceof AnyType)
				elements.add(AnyType.getInstance());
			else if (alternative instanceof ArrayType array)
				elements.add(absentAsNull(elementAt(array, index)));
			else if (alternative instanceof NullType)
				elements.add(NullType.getInstance());
			else
				throw new Problem("Cannot match array pattern against " + alternative);
		}
		return UnionType.of(elements);
	}

	/**
	 * The value an object destructuring pattern extracts for {@code key}, or for an unknown key when
	 * {@code key} is null. An absent key supplies null, as {@code ObjectMatcher} does.
	 */
	static Type objectPatternValue(Type input, @Nullable String key) {
		List<Type> values = new ArrayList<>();
		for (Type alternative : alternatives(input)) {
			if (alternative instanceof AnyType)
				values.add(AnyType.getInstance());
			else if (alternative instanceof ObjectType object)
				values.add(key != null ? field(object, key) : objectMemberOrNull(object));
			else if (alternative instanceof NullType)
				values.add(NullType.getInstance());
		}
		// As with an array pattern: only a value no alternative admits is an error.
		if (values.isEmpty())
			throw new Problem("Cannot match object pattern against " + input);
		return UnionType.of(values);
	}

	static Type withoutNull(Type type) {
		List<Type> alternatives = new ArrayList<>();
		for (Type alternative : alternatives(type)) {
			if (!(alternative instanceof NullType))
				alternatives.add(alternative);
		}
		return UnionType.of(alternatives);
	}

	/**
	 * What a type says about the branch a condition of that type takes. jq counts only {@code null} and
	 * {@code false} as falsy, so every other kind of value is truthy outright and a type admitting no
	 * value of those two kinds decides the branch on its own.
	 * <p>
	 * A union decides only when every alternative decides the same way. A type that says nothing about
	 * its values -- {@link AnyType}, a variable, a recursive shape -- decides nothing.
	 */
	static Truthiness truthiness(Type type) {
		@Var
		@Nullable Truthiness result = null;
		for (Type alternative : alternatives(type)) {
			Truthiness truthiness = alternativeTruthiness(alternative);
			if (truthiness == Truthiness.UNKNOWN || (result != null && result != truthiness))
				return Truthiness.UNKNOWN;
			result = truthiness;
		}
		return result != null ? result : Truthiness.UNKNOWN;
	}

	private static Truthiness alternativeTruthiness(Type alternative) {
		if (alternative instanceof NullType)
			return Truthiness.ALWAYS_FALSE;
		if (alternative instanceof BooleanType bool) {
			@Nullable Boolean value = bool.value();
			if (value == null)
				return Truthiness.UNKNOWN;
			return value ? Truthiness.ALWAYS_TRUE : Truthiness.ALWAYS_FALSE;
		}
		if (alternative instanceof StringType || alternative instanceof NumericType
				|| alternative instanceof BinaryType || alternative instanceof ArrayType
				|| alternative instanceof ObjectType)
			return Truthiness.ALWAYS_TRUE;
		return Truthiness.UNKNOWN;
	}

	/**
	 * Whether no value inhabits both types, so that jq's {@code ==} can never call a value of one equal
	 * to a value of the other.
	 * <p>
	 * This is answered from what the types say outright, so it is conservative in one direction only: a
	 * type that says nothing about its values, and two types of the same kind that are not both known
	 * values, are never called disjoint. Two containers are not compared element by element either --
	 * proving {@code [INT]} and {@code [STRING]} disjoint is not worth a rule of its own here.
	 */
	static boolean disjoint(Type left, Type right) {
		for (Type leftAlternative : alternatives(left)) {
			for (Type rightAlternative : alternatives(right)) {
				if (!disjointAlternatives(leftAlternative, rightAlternative))
					return false;
			}
		}
		return true;
	}

	private static boolean disjointAlternatives(Type left, Type right) {
		// Nothing inhabits NEVER, so it shares no value with anything, itself included.
		if (left == NeverType.getInstance() || right == NeverType.getInstance())
			return true;
		if (left instanceof AnyType || right instanceof AnyType
				|| left instanceof TypeVariable || right instanceof TypeVariable
				|| left instanceof RecursiveType || right instanceof RecursiveType)
			return false;
		// A binary node is a provider's own kind of value, and what it compares equal to is the
		// provider's business rather than something worth ruling on here.
		if (left instanceof BinaryType || right instanceof BinaryType)
			return false;
		if (left instanceof StringType leftString && right instanceof StringType rightString)
			return differingValues(leftString.value(), rightString.value());
		if (left instanceof BooleanType leftBoolean && right instanceof BooleanType rightBoolean)
			return differingValues(leftBoolean.value(), rightBoolean.value());
		// Two values of one kind may well be equal, and for a number the kind is only a hint anyway.
		return left.getClass() != right.getClass();
	}

	private static boolean differingValues(@Nullable Object left, @Nullable Object right) {
		return left != null && right != null && !left.equals(right);
	}

	private static Type pairs(Type left, Type right, PairRule rule) {
		// An operand that emits nothing never reaches the operator, so the operator has no answer to give
		// rather than an answer this should reject. A loop body being analysed before its recursive call
		// has a type is the ordinary way one gets here.
		if (left == NeverType.getInstance() || right == NeverType.getInstance())
			return NeverType.getInstance();
		List<Type> outputs = new ArrayList<>();
		for (Type l : alternatives(left)) {
			for (Type r : alternatives(right))
				outputs.add(rule.apply(l, r));
		}
		return UnionType.of(outputs);
	}

	@FunctionalInterface
	private interface PairRule {
		Type apply(Type left, Type right);
	}

	static final class Problem extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * The call this problem is about, as {@code name/arity}, when it is one -- so the trace a diagnostic
		 * carries can name it alongside the calls it was reached through. Null for a problem an operator or a
		 * path expression raised, which is not a call at all.
		 */
		private final @Nullable String callee;

		Problem(String message) {
			this(message, null);
		}

		Problem(String message, @Nullable String callee) {
			super(message);
			this.callee = callee;
		}

		@Nullable
		String callee() {
			return callee;
		}
	}
}
