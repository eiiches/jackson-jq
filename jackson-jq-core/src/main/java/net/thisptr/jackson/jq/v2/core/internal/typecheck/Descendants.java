package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * The transitive closure {@code ..} walks: the input together with every type reachable below it.
 * <p>
 * Before jq 1.6, {@code ..} was {@code recurse(.[]?; . != null)}, so a null <em>reached by descending</em>
 * is not emitted -- while the root is emitted whatever it is, because {@code recurse}'s body emits
 * {@code .} before it filters. {@code RecursionOperator} records which of the two the compiled query means;
 * {@code visitsNullValues} carries it here.
 * <p>
 * The closure terminates because a {@link RecursiveType} is unfolded once before it is walked, which makes
 * every occurrence of its binder denote that same recursive type -- already visited by then. The result is
 * that unfolding rather than the recursive type itself, which is the equi-recursively equal spelling that
 * does not leave the union carrying both.
 */
final class Descendants {
	private Descendants() {
	}

	static Type of(Type input, boolean visitsNullValues) {
		List<Type> collected = new ArrayList<>();
		collect(input, true, visitsNullValues, collected, new HashSet<>());
		return UnionType.of(collected);
	}

	private static void collect(Type type, boolean root, boolean visitsNullValues, List<Type> out, Set<Type> visited) {
		if (type == NeverType.getInstance() || type == UndefinedType.getInstance())
			return;
		if (!root && !visitsNullValues && type instanceof NullType)
			return;
		if (type instanceof UnionType union) {
			for (Type alternative : union.alternatives())
				collect(alternative, root, visitsNullValues, out, visited);
			return;
		}
		if (!visited.add(type))
			return;
		if (type instanceof RecursiveType recursive) {
			collect(unfold(recursive), root, visitsNullValues, out, visited);
			return;
		}
		out.add(type);
		if (type instanceof AnyType)
			return; // ANY already stands for every value, so its descendants add nothing.
		if (type instanceof ArrayType array) {
			collect(array.elementType(), false, visitsNullValues, out, visited);
		} else if (type instanceof ObjectType object) {
			for (Type field : object.fields().values())
				collect(field, false, visitsNullValues, out, visited);
			collect(object.additionalFieldType(), false, visitsNullValues, out, visited);
		}
	}

	/**
	 * Replaces the binder's occurrences in the body with the recursive type itself, which an equi-recursive
	 * type permits by definition.
	 */
	private static Type unfold(RecursiveType recursive) {
		TypeVariable variable = recursive.variable();
		return TypeSubstitution.apply(recursive.body(), Set.of(variable), Map.of(variable, recursive));
	}
}
