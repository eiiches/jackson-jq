package net.thisptr.jackson.jq.v2.json.internal.io;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatOptionsTest {
	@Test
	void builderDefaultsShareAnInstance() {
		FormatOptions first = FormatOptions.newBuilder().build();
		assertThat(first.getLowerCaseDecimalExponent()).isFalse();
		assertThat(first.getRoundNumbersToDouble()).isFalse();
		assertThat(FormatOptions.newBuilder().build()).isSameAs(first);
		assertThat(first.toBuilder().build()).isSameAs(first);
	}

	@Test
	void toBuilderCarriesBothSettings() {
		FormatOptions original = FormatOptions.newBuilder()
				.setLowerCaseDecimalExponent(true)
				.setRoundNumbersToDouble(true)
				.build();
		FormatOptions copy = original.toBuilder().build();
		assertThat(copy.getLowerCaseDecimalExponent()).isTrue();
		assertThat(copy.getRoundNumbersToDouble()).isTrue();
		assertThat(copy.toBuilder().setRoundNumbersToDouble(false).build().getLowerCaseDecimalExponent()).isTrue();
		assertThat(original.getRoundNumbersToDouble()).isTrue();
	}
}
