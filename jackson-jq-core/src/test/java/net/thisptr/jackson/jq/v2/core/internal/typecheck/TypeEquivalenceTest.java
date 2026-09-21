package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;

class TypeEquivalenceTest {
	@Test
	void recursiveTypesAreEqualUpToRenaming() {
		TypeVariable t = TypeVariable.of("T");
		TypeVariable node = TypeVariable.of("Node");
		RecursiveType left = RecursiveType.of(t, UnionType.of(NullType.getInstance(), ArrayType.of(t)));
		RecursiveType right = RecursiveType.of(node, UnionType.of(NullType.getInstance(), ArrayType.of(node)));

		// The two are not equal -- equals is structural -- but they denote the same type.
		assertThat(left).isNotEqualTo(right);
		assertThat(TypeEquivalence.isEqualType(left, right)).isTrue();
		assertThat(TypeEquivalence.isEqualType(left, left)).isTrue();
		assertThat(TypeEquivalence.isEqualType(left, RecursiveType.of(node, UnionType.of(StringType.getInstance(), ArrayType.of(node))))).isFalse();
		assertThat(TypeEquivalence.isEqualType(StringType.getInstance(), StringType.getInstance())).isTrue();
		assertThat(TypeEquivalence.isEqualType(StringType.getInstance(), NumericType.getInstance())).isFalse();
	}

	@Test
	void freeVariablesAreComparedByValue() {
		TypeVariable t = TypeVariable.of("T");
		TypeVariable node = TypeVariable.of("Node");
		TypeVariable free = TypeVariable.of("X");

		assertThat(TypeEquivalence.isEqualType(RecursiveType.of(t, (Type) ArrayType.of(UnionType.of(t, free))),
				RecursiveType.of(node, (Type) ArrayType.of(UnionType.of(node, free))))).isTrue();
		assertThat(TypeEquivalence.isEqualType(RecursiveType.of(t, (Type) ArrayType.of(UnionType.of(t, free))),
				RecursiveType.of(node, (Type) ArrayType.of(UnionType.of(node, TypeVariable.of("Y")))))).isFalse();
		// A bound variable never matches a free one, whatever it is called.
		assertThat(TypeEquivalence.isEqualType(RecursiveType.of(t, (Type) ArrayType.of(t)), ArrayType.of(t))).isFalse();
	}

	@Test
	void shadowedBindersResolveToTheInnermost() {
		TypeVariable t = TypeVariable.of("T");
		TypeVariable u = TypeVariable.of("U");
		// An inner binder reusing the outer name: every reference below it is the inner variable.
		Type shadowing = RecursiveType.of(t, (Type) ArrayType.of(UnionType.of(t, RecursiveType.of(t, (Type) ArrayType.of(t)))));
		Type renamed = RecursiveType.of(t, (Type) ArrayType.of(UnionType.of(t, RecursiveType.of(u, (Type) ArrayType.of(u)))));
		Type captured = RecursiveType.of(t, (Type) ArrayType.of(UnionType.of(t, RecursiveType.of(u, (Type) ArrayType.of(UnionType.of(t, u))))));

		assertThat(TypeEquivalence.isEqualType(shadowing, renamed)).isTrue();
		assertThat(TypeEquivalence.isEqualType(shadowing, captured)).isFalse();
	}
}
