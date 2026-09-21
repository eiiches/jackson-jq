package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.List;

import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NeverType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a loop accumulator does with an array's known positions, which is what decides whether it settles.
 */
class StructuralWideningTest {
	@Test
	void arraysDescribingTheSamePositionsJoinPositionByPosition() {
		// `limit` re-derives `[count, item]` on every round, and this is what lets it settle as a pair
		// rather than as an array of count-or-item.
		Type first = ArrayType.of(List.of(NumericType.getInstance(), NullType.getInstance()));
		Type second = ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()));
		assertThat(StructuralWidening.join(first, second))
				.isEqualTo(ArrayType.of(List.of(NumericType.getInstance(), UnionType.of(NullType.getInstance(), StringType.getInstance()))));
	}

	@Test
	void arraysDescribingDifferentlyManyPositionsKeepNone() {
		// `reduce .[] as $x ([]; . + [$x])` describes one more position every round; dropping them the
		// moment the counts disagree is what makes the sequence finite.
		Type one = ArrayType.of(List.of(NumericType.getInstance()));
		Type two = ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance()));
		assertThat(StructuralWidening.join(one, two)).isEqualTo(ArrayType.of(NumericType.getInstance()));
		// And having dropped them, it stays dropped.
		assertThat(StructuralWidening.join(ArrayType.of(NumericType.getInstance()), two)).isEqualTo(ArrayType.of(NumericType.getInstance()));
	}

	@Test
	void anAppendingAccumulatorSettlesInThreeRounds() {
		@Var
		Type accumulator = ArrayType.of(NeverType.getInstance());
		for (int i = 0; i < 4; i++) {
			Type next = StructuralWidening.join(accumulator,
					TypeRelations.plus(accumulator, ArrayType.of(List.of(NumericType.getInstance()))));
			if (TypeEquivalence.isEqualType(next, accumulator)) {
				assertThat(accumulator).isEqualTo(ArrayType.of(NumericType.getInstance()));
				return;
			}
			accumulator = next;
		}
		throw new AssertionError("The accumulator never settled: " + accumulator);
	}

	@Test
	void widenDifferencesAlsoOnlyPairsUpEquallyLongArrays() {
		assertThat(StructuralWidening.widenDifferences(ArrayType.of(List.of(NumericType.getInstance(), NumericType.getInstance())),
				ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()))))
				.isEqualTo(ArrayType.of(List.of(NumericType.getInstance(), AnyType.getInstance())));
		// Unequal lengths drop to the element types, which still agree more than ANY would.
		assertThat(StructuralWidening.widenDifferences(ArrayType.of(List.of(NumericType.getInstance())),
				ArrayType.of(List.of(NumericType.getInstance(), StringType.getInstance()))))
				.isEqualTo(ArrayType.of(UnionType.of(NumericType.getInstance(), StringType.getInstance())));
	}
}
