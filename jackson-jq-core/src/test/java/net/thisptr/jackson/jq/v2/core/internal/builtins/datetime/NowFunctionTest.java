package net.thisptr.jackson.jq.v2.core.internal.builtins.datetime;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NowFunctionTest {
	@Test
	void convertsTimeToEpochSecondsAtMicrosecondPrecision() {
		Instant instant = Instant.ofEpochSecond(1_790_700_612L, 751_727_999);
		assertThat(NowFunction.epochSeconds(instant)).isEqualTo(1_790_700_612.751727);
	}
}
