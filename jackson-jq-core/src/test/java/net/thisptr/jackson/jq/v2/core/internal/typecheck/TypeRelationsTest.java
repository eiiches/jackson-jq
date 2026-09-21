package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules that decide where an array's known positions survive an operation and where they cannot.
 */
class TypeRelationsTest {
	private static final Type PAIR = ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()));

	@Test
	void concatenationShiftsTheRightOperandByTheLeftsLength() {
		assertThat(TypeRelations.plus(PAIR, ArrayType.of(List.of(BooleanType.getInstance()))))
				.isEqualTo(ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance(), BooleanType.getInstance())));
	}

	@Test
	void concatenationOntoAnUnknownLengthKeepsNoPosition() {
		// Where the right operand's elements land is the left operand's length, which this does not know.
		assertThat(TypeRelations.plus(ArrayType.of(NumericType.getInstance()), ArrayType.of(List.of(StringType.getInstance()))))
				.isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
		Type openPair = ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance())), NeverType.getInstance());
		assertThat(TypeRelations.plus(openPair, ArrayType.of(List.of(StringType.getInstance()))))
				.isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
	}

	@Test
	void subtractionAndSlicingKeepNoPosition() {
		// Both remove elements, so nothing is left at the position it was written at.
		assertThat(TypeRelations.minus(PAIR, ArrayType.of(NumericType.getInstance())))
				.isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
		assertThat(TypeRelations.slice(PAIR)).isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
	}

	@Test
	void iterationAnswersEveryElementWithoutSayingWhichArePresent() {
		assertThat(TypeRelations.iterate(PAIR)).isEqualTo(UnionType.of(NumericType.getInstance(), StringType.getInstance()));
		assertThat(TypeRelations.iterate(ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance())))))
				.isSameAs(NumericType.getInstance());
	}

	@Test
	void aKnownPositionIsReachedAndOneBeyondThemIsNull() {
		assertThat(TypeRelations.arrayPatternElement(PAIR, 0)).isSameAs(NumericType.getInstance());
		assertThat(TypeRelations.arrayPatternElement(PAIR, 1)).isSameAs(StringType.getInstance());
		assertThat(TypeRelations.arrayPatternElement(PAIR, 2)).isSameAs(NullType.getInstance());
		assertThat(TypeRelations.arrayPatternElement(ArrayType.of(NumericType.getInstance()), 0))
				.isEqualTo(UnionType.of(NumericType.getInstance(), NullType.getInstance()));
	}

	@Test
	void aValueNoArrayPatternCanMatchIsAnError() {
		assertThatThrownBy(() -> TypeRelations.arrayPatternElement(StringType.getInstance(), 0))
				.isInstanceOf(TypeRelations.Problem.class).hasMessageContaining("Cannot match array pattern");
		assertThat(TypeRelations.arrayPatternElement(UnionType.of(PAIR, NullType.getInstance()), 0))
				.isEqualTo(UnionType.of(NumericType.getInstance(), NullType.getInstance()));
	}

	@Test
	void onlyAnArrayOfKnownLengthHasAnExactLength() {
		assertThat(TypeRelations.exactLength(ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance())))).hasValue(2);
		assertThat(TypeRelations.exactLength(ArrayType.of(NumericType.getInstance()))).isEmpty();
		assertThat(TypeRelations.exactLength(ArrayType.of(List.of(NumericType.getInstance()), StringType.getInstance()))).isEmpty();
		assertThat(TypeRelations.exactLength(ArrayType.of(List.of(UnionType.of(NumericType.getInstance(), UndefinedType.getInstance()))))).isEmpty();
	}
}
