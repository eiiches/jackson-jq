package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.RecursiveType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;

class TypeSubstitutionTest {
	@Test
	void substitutesFreeVariablesInsideRecursiveTypes() {
		TypeVariable value = TypeVariable.of("Value");
		TypeVariable recursive = TypeVariable.of("Recursive");
		Type source = RecursiveType.of(recursive, (Type) ObjectType.of(
				"next", UnionType.of(recursive, NullType.getInstance()),
				"value", value));
		Type expected = RecursiveType.of(recursive, (Type) ObjectType.of(
				"next", UnionType.of(recursive, NullType.getInstance()),
				"value", StringType.getInstance()));

		assertThat(substitute(source, value, StringType.getInstance())).isEqualTo(expected);
	}

	@Test
	void recursiveBindersShadowSchemeVariables() {
		TypeVariable variable = TypeVariable.of("T");
		RecursiveType recursive = RecursiveType.of(variable, (Type) ArrayType.of(variable));

		assertThat(substitute(recursive, variable, StringType.getInstance())).isSameAs(recursive);
	}

	@Test
	void nestedRecursiveBindersResolveToTheInnermostBinder() {
		TypeVariable variable = TypeVariable.of("T");
		Type nested = RecursiveType.of(variable, (Type) ArrayType.of(UnionType.of(variable, RecursiveType.of(variable, (Type) ArrayType.of(variable)))));

		assertThat(substitute(nested, variable, StringType.getInstance())).isSameAs(nested);
	}

	@Test
	void traversesAllContainerTypes() {
		TypeVariable variable = TypeVariable.of("T");
		Type source = ObjectType.of(Map.of("array", ArrayType.of(variable)), UnionType.of(variable, NullType.getInstance()));
		Type expected = ObjectType.of(Map.of("array", ArrayType.of(StringType.getInstance())), UnionType.of(StringType.getInstance(), NullType.getInstance()));

		assertThat(substitute(source, variable, StringType.getInstance())).isEqualTo(expected);
	}

	@Test
	void leavesUnquantifiedVariablesAndLeavesUnchanged() {
		TypeVariable quantified = TypeVariable.of("Quantified");
		TypeVariable free = TypeVariable.of("Free");

		assertThat(TypeSubstitution.apply(free, Set.of(quantified), Map.of(quantified, StringType.getInstance()))).isSameAs(free);
		assertThat(TypeSubstitution.apply(NumericType.getInstance(), Set.of(quantified), Map.of(quantified, StringType.getInstance())))
				.isSameAs(NumericType.getInstance());
	}

	@Test
	void missingSubstitutionsFallBackToSubstitutedUpperBounds() {
		TypeVariable unbounded = TypeVariable.of("Unbounded");
		assertThat(TypeSubstitution.apply(unbounded, Set.of(unbounded), Map.of())).isSameAs(AnyType.getInstance());

		TypeVariable string = TypeVariable.of("String");
		assertThat(TypeSubstitution.apply(string, Set.of(string), Map.of(), Map.of(string, StringType.getInstance()))).isSameAs(StringType.getInstance());

		TypeVariable element = TypeVariable.of("Element");
		TypeVariable array = TypeVariable.of("Array");
		assertThat(TypeSubstitution.apply(array, Set.of(element, array), Map.of(element, StringType.getInstance()), Map.of(array, ArrayType.of(element))))
				.isEqualTo(ArrayType.of(StringType.getInstance()));
	}

	private static Type substitute(Type source, TypeVariable variable, Type replacement) {
		return TypeSubstitution.apply(source, Set.of(variable), Map.of(variable, replacement));
	}
}
