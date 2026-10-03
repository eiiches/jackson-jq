package net.thisptr.jackson.jq.v2.json.internal.io;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatOptionsTest {
	@Test
	void builderDefaultsShareAnInstance() {
		FormatOptions first = FormatOptions.newBuilder().build();
		assertThat(first.getLowerCaseDecimalExponent()).isFalse();
		assertThat(first.getRoundNumbersToDouble()).isFalse();
		assertThat(first.getMaxLength()).isEqualTo(Integer.MAX_VALUE);
		assertThat(FormatOptions.newBuilder().build()).isSameAs(first);
		assertThat(first.toBuilder().build()).isSameAs(first);
		assertThat(FormatOptions.newBuilder().setMaxLength(Integer.MAX_VALUE).build()).isSameAs(first);
	}

	@Test
	void toBuilderCarriesEverySetting() {
		FormatOptions original = FormatOptions.newBuilder()
				.setLowerCaseDecimalExponent(true)
				.setRoundNumbersToDouble(true)
				.setMaxLength(64)
				.build();
		FormatOptions copy = original.toBuilder().build();
		assertThat(copy.getLowerCaseDecimalExponent()).isTrue();
		assertThat(copy.getRoundNumbersToDouble()).isTrue();
		assertThat(copy.getMaxLength()).isEqualTo(64);
		assertThat(copy.toBuilder().setRoundNumbersToDouble(false).build().getLowerCaseDecimalExponent()).isTrue();
		assertThat(original.getRoundNumbersToDouble()).isTrue();
	}

	@Test
	void aNegativeMaxLengthIsRejected() {
		assertThatThrownBy(() -> FormatOptions.newBuilder().setMaxLength(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maxLength must not be negative");
	}
}
