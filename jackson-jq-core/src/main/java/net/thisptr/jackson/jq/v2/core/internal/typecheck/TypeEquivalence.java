package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Compares types up to renaming of the variables bound by {@link RecursiveType}.
 */
final class TypeEquivalence {
	private TypeEquivalence() {
	}

	static boolean isEqualType(Type left, Type right) {
		return equivalent(left, right, new ArrayDeque<>(), new ArrayDeque<>());
	}

	private static boolean equivalent(Type left, Type right,
									  Deque<TypeVariable> leftBinders, Deque<TypeVariable> rightBinders) {
		if (left instanceof TypeVariable || right instanceof TypeVariable) {
			if (!(left instanceof TypeVariable leftVariable) || !(right instanceof TypeVariable rightVariable))
				return false;
			int leftIndex = binderIndex(leftBinders, leftVariable);
			int rightIndex = binderIndex(rightBinders, rightVariable);
			if (leftIndex < 0 || rightIndex < 0)
				return leftIndex == rightIndex && leftVariable.equals(rightVariable);
			return leftIndex == rightIndex;
		}
		if (left instanceof RecursiveType || right instanceof RecursiveType) {
			if (!(left instanceof RecursiveType leftRecursive) || !(right instanceof RecursiveType rightRecursive))
				return false;
			leftBinders.push(leftRecursive.variable());
			rightBinders.push(rightRecursive.variable());
			try {
				return equivalent(leftRecursive.body(), rightRecursive.body(), leftBinders, rightBinders);
			} finally {
				leftBinders.pop();
				rightBinders.pop();
			}
		}
		if (left instanceof ArrayType || right instanceof ArrayType) {
			if (!(left instanceof ArrayType leftArray) || !(right instanceof ArrayType rightArray)
					|| leftArray.knownElements().size() != rightArray.knownElements().size())
				return false;
			for (int i = 0; i < leftArray.knownElements().size(); i++) {
				if (!equivalent(leftArray.knownElements().get(i), rightArray.knownElements().get(i),
						leftBinders, rightBinders))
					return false;
			}
			return equivalent(leftArray.additionalElementType(), rightArray.additionalElementType(),
					leftBinders, rightBinders);
		}
		if (left instanceof ObjectType || right instanceof ObjectType) {
			if (!(left instanceof ObjectType leftObject) || !(right instanceof ObjectType rightObject)
					|| !leftObject.fields().keySet().equals(rightObject.fields().keySet()))
				return false;
			for (String field : leftObject.fields().keySet()) {
				if (!equivalent(Objects.requireNonNull(leftObject.fields().get(field)),
						Objects.requireNonNull(rightObject.fields().get(field)), leftBinders, rightBinders))
					return false;
			}
			return equivalent(leftObject.additionalFieldType(), rightObject.additionalFieldType(), leftBinders, rightBinders);
		}
		if (left instanceof UnionType || right instanceof UnionType) {
			if (!(left instanceof UnionType leftUnion) || !(right instanceof UnionType rightUnion)
					|| leftUnion.alternatives().size() != rightUnion.alternatives().size())
				return false;
			// Alternatives are in canonical order by their display, which renaming can change, so pair
			// them up instead of comparing position by position.
			boolean[] paired = new boolean[rightUnion.alternatives().size()];
			for (Type leftAlternative : leftUnion.alternatives()) {
				@Var
				int match = -1;
				for (int i = 0; i < paired.length; i++) {
					if (!paired[i] && equivalent(leftAlternative, rightUnion.alternatives().get(i), leftBinders, rightBinders)) {
						match = i;
						break;
					}
				}
				if (match < 0)
					return false;
				paired[match] = true;
			}
			return true;
		}
		return left.equals(right);
	}

	/**
	 * Returns how many binders enclose the innermost one that binds {@code variable}, or {@code -1}
	 * when the variable is free.
	 */
	private static int binderIndex(Deque<TypeVariable> binders, TypeVariable variable) {
		@Var
		int index = 0;
		for (TypeVariable binder : binders) {
			if (binder.equals(variable))
				return index;
			index++;
		}
		return -1;
	}
}
