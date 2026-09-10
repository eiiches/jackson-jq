package net.thisptr.jackson.jq.v2.core.internal.commons.range;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LongRangeTest {

	@Test
	void testValidRange() {
		LongRange range = new LongRange(1, 5);
		assertThat(range.startInclusive).isEqualTo(1);
		assertThat(range.endExclusive).isEqualTo(5);
		assertThat(range.length()).isEqualTo(4);
		assertThat(range.isEmpty()).isFalse();
	}

	@Test
	void testEmptyRange() {
		LongRange range = LongRange.of(3, 3);
		assertThat(range.startInclusive).isEqualTo(3);
		assertThat(range.endExclusive).isEqualTo(3);
		assertThat(range.length()).isEqualTo(0);
		assertThat(range.isEmpty()).isTrue();
	}

	@Test
	void testRejectsInvalidRange() {
		assertThatThrownBy(() -> new LongRange(5, 4))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("startInclusive (5) must not be greater than endExclusive (4)");

		assertThatThrownBy(() -> LongRange.of(10, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testContains() {
		LongRange range = LongRange.of(2, 6);
		assertThat(range.contains(1)).isFalse();
		assertThat(range.contains(2)).isTrue();
		assertThat(range.contains(5)).isTrue();
		assertThat(range.contains(6)).isFalse();
		assertThat(range.contains(7)).isFalse();

		LongRange empty = LongRange.of(3, 3);
		assertThat(empty.contains(3)).isFalse();
	}

	@Test
	void testEqualsAndHashCode() {
		LongRange a = LongRange.of(1, 4);
		LongRange b = new LongRange(1, 4);
		LongRange c = LongRange.of(1, 5);
		LongRange d = LongRange.of(2, 4);

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(d);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("not a range");
	}

	@Test
	void testToString() {
		assertThat(LongRange.of(0, 10)).hasToString("[0, 10)");
		assertThat(LongRange.of(-5, 5)).hasToString("[-5, 5)");
	}
}
