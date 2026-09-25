package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Drives a loop's accumulator type to a fixed point.
 * <p>
 * An accumulator that grows -- {@code reduce .[] as $x (null; {v: .})} nests one object deeper on every
 * iteration -- never stabilises by unioning alone. {@link #join} keeps the shape instead of widening it to
 * a union, and {@link #fold} closes a shape that has started to embed its own previous iterate into a
 * {@link RecursiveType}, which is the finite spelling of that growth. What neither can settle,
 * {@link #widenDifferences} gives up on one subtree at a time rather than collapsing the whole
 * accumulator to {@code ANY}.
 */
final class StructuralWidening {
	private StructuralWidening() {
	}

	/**
	 * Unions {@code left} and {@code right}, except that two arrays or two objects join pointwise. Joining
	 * inside the container rather than around it is what lets an accumulator that keeps rebuilding the same
	 * shape settle, instead of growing one union alternative per iteration.
	 */
	static Type join(Type left, Type right) {
		if (TypeEquivalence.isEqualType(left, right))
			return left;
		if (left == NeverType.getInstance())
			return right;
		if (right == NeverType.getInstance())
			return left;
		// A recursive type and the shape it stands for are the same type, but only one of the two spellings
		// joins structurally with anything. Unfolding first is what stops a settled accumulator from
		// alternating between the two and never comparing equal to itself.
		if (left instanceof RecursiveType recursive)
			return join(unfold(recursive), right);
		if (right instanceof RecursiveType recursive)
			return join(left, unfold(recursive));
		if (left instanceof ArrayType leftArray && right instanceof ArrayType rightArray)
			return joinArrays(leftArray, rightArray);
		if (left instanceof ObjectType leftObject && right instanceof ObjectType rightObject)
			return joinObjects(leftObject, rightObject);
		if (left instanceof UnionType || right instanceof UnionType)
			return joinUnions(left, right);
		return UnionType.of(left, right);
	}

	/**
	 * Joins two arrays position by position, but only while they agree on how many positions there are.
	 * An accumulator that appends an element per iteration -- {@code reduce .[] as $x ([]; . + [$x])} --
	 * describes one more position every time and would never settle; dropping to a plain array the moment
	 * the counts disagree is what makes the sequence finite, since the count only ever falls.
	 */
	private static Type joinArrays(ArrayType left, ArrayType right) {
		if (left.knownElements().size() != right.knownElements().size())
			return ArrayType.of(join(left.elementType(), right.elementType()));
		List<Type> knownElements = new ArrayList<>(left.knownElements().size());
		for (int i = 0; i < left.knownElements().size(); i++)
			knownElements.add(join(left.knownElements().get(i), right.knownElements().get(i)));
		return ArrayType.of(knownElements, join(left.additionalElementType(), right.additionalElementType()));
	}

	private static Type joinObjects(ObjectType left, ObjectType right) {
		Map<String, Type> fields = new TreeMap<>(left.fields());
		for (Map.Entry<String, Type> field : right.fields().entrySet())
			fields.merge(field.getKey(), field.getValue(), StructuralWidening::join);
		return ObjectType.of(fields, join(left.additionalFieldType(), right.additionalFieldType()));
	}

	/**
	 * Joins each alternative of {@code right} into the one alternative of {@code left} that has its shape,
	 * so that two unions differing only inside a shared container join in place rather than accumulating a
	 * second alternative of the same kind.
	 */
	private static Type joinUnions(Type left, Type right) {
		List<Type> alternatives = new ArrayList<>(TypeRelations.alternatives(left));
		for (Type candidate : TypeRelations.alternatives(right)) {
			@Var int match = -1;
			for (int i = 0; i < alternatives.size(); i++) {
				if (sameShape(alternatives.get(i), candidate)) {
					match = i;
					break;
				}
			}
			if (match < 0)
				alternatives.add(candidate);
			else
				alternatives.set(match, join(alternatives.get(match), candidate));
		}
		return UnionType.of(alternatives);
	}

	private static boolean sameShape(Type left, Type right) {
		if (left instanceof ArrayType && right instanceof ArrayType)
			return true;
		if (left instanceof ObjectType && right instanceof ObjectType)
			return true;
		return TypeEquivalence.isEqualType(left, right);
	}

	/**
	 * Closes {@code candidate} into a recursive type when it has come to embed {@code previous}, the shape
	 * it grew out of. Returns {@code candidate} unchanged when it does not, or when {@code previous} also
	 * occurs at the top level: an occurrence not below a container is not a loop that can be closed, only a
	 * type that happens to appear on both sides.
	 */
	static Type fold(Type previous, Type candidate, Supplier<TypeVariable> freshVariable) {
		if (!(previous instanceof ArrayType || previous instanceof ObjectType || previous instanceof UnionType
				|| previous instanceof RecursiveType))
			return candidate;
		if (TypeEquivalence.isEqualType(previous, candidate) || occursUnguarded(previous, candidate))
			return candidate;
		TypeVariable variable = freshVariable.get();
		boolean[] replaced = new boolean[1];
		Type body = replace(previous, variable, candidate, replaced, new HashSet<>());
		if (!replaced[0])
			return candidate;
		return RecursiveType.of(variable, body);
	}

	private static Type unfold(RecursiveType recursive) {
		TypeVariable variable = recursive.variable();
		return TypeSubstitution.apply(recursive.body(), Set.of(variable), Map.of(variable, recursive));
	}

	private static boolean occursUnguarded(Type previous, Type candidate) {
		for (Type alternative : TypeRelations.alternatives(candidate)) {
			if (TypeEquivalence.isEqualType(previous, alternative))
				return true;
		}
		return false;
	}

	private static Type replace(Type previous, TypeVariable variable, Type type, boolean[] replaced, Set<Type> visited) {
		if (TypeEquivalence.isEqualType(previous, type)) {
			replaced[0] = true;
			return variable;
		}
		if (!visited.add(type))
			return type;
		if (type instanceof ArrayType array) {
			List<Type> knownElements = new ArrayList<>(array.knownElements().size());
			for (Type knownElement : array.knownElements())
				knownElements.add(replace(previous, variable, knownElement, replaced, visited));
			return ArrayType.of(knownElements, replace(previous, variable, array.additionalElementType(), replaced, visited));
		}
		if (type instanceof ObjectType object) {
			Map<String, Type> fields = new LinkedHashMap<>();
			for (Map.Entry<String, Type> field : object.fields().entrySet())
				fields.put(field.getKey(), replace(previous, variable, field.getValue(), replaced, visited));
			return ObjectType.of(fields, replace(previous, variable, object.additionalFieldType(), replaced, visited));
		}
		if (type instanceof UnionType union) {
			List<Type> alternatives = new ArrayList<>();
			for (Type alternative : union.alternatives())
				alternatives.add(replace(previous, variable, alternative, replaced, visited));
			return UnionType.of(alternatives);
		}
		return type;
	}

	/**
	 * Keeps everything the two iterates agree on and replaces only what still differs with {@code ANY}. A
	 * loop whose accumulator refuses to settle is usually settled everywhere but one field; widening that
	 * field is honest, and widening the whole accumulator throws away what was known.
	 */
	static Type widenDifferences(Type previous, Type candidate) {
		return widenDifferences(previous, candidate, new HashSet<>());
	}

	private static Type widenDifferences(Type previous, Type candidate, Set<Type> visited) {
		if (TypeEquivalence.isEqualType(previous, candidate))
			return candidate;
		if (!visited.add(candidate))
			return candidate;
		if (previous instanceof ArrayType previousArray && candidate instanceof ArrayType candidateArray) {
			if (previousArray.knownElements().size() != candidateArray.knownElements().size())
				return ArrayType.of(widenDifferences(previousArray.elementType(), candidateArray.elementType(), visited));
			List<Type> knownElements = new ArrayList<>(previousArray.knownElements().size());
			for (int i = 0; i < previousArray.knownElements().size(); i++) {
				knownElements.add(widenDifferences(previousArray.knownElements().get(i),
						candidateArray.knownElements().get(i), visited));
			}
			return ArrayType.of(knownElements, widenDifferences(previousArray.additionalElementType(),
					candidateArray.additionalElementType(), visited));
		}
		if (previous instanceof ObjectType previousObject && candidate instanceof ObjectType candidateObject) {
			Map<String, Type> fields = new TreeMap<>();
			for (Map.Entry<String, Type> field : candidateObject.fields().entrySet()) {
				Type before = previousObject.fields().get(field.getKey());
				fields.put(field.getKey(), before == null ? AnyType.getInstance()
						: widenDifferences(before, field.getValue(), visited));
			}
			return ObjectType.of(fields, widenDifferences(previousObject.additionalFieldType(), candidateObject.additionalFieldType(), visited));
		}
		if (previous instanceof UnionType || candidate instanceof UnionType) {
			List<Type> alternatives = new ArrayList<>();
			for (Type alternative : TypeRelations.alternatives(candidate)) {
				@Nullable Type before = matching(previous, alternative);
				alternatives.add(before == null ? alternative : widenDifferences(before, alternative, visited));
			}
			return UnionType.of(alternatives);
		}
		return AnyType.getInstance();
	}

	private static @Nullable Type matching(Type previous, Type candidate) {
		for (Type alternative : TypeRelations.alternatives(previous)) {
			if (sameShape(alternative, candidate))
				return alternative;
		}
		return null;
	}
}
