package net.thisptr.jackson.jq.v2.core.internal.misc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonNodeUtilsTest {
	private final JsonProvider<JsonNode> jsonProvider = Jackson2JsonProviderImpl.getInstance();

	@Test
	void testDoubleAtLongUpperBoundaryRemainsDouble() {
		JsonNode node = JsonNodeUtils.asNumericNode(jsonProvider, 0x1p63);

		assertThat(node.isDouble()).isTrue();
		assertThat(jsonProvider.asDouble(node)).isEqualTo(0x1p63);
	}

	@Test
	void testDoubleWithinLongRangeBecomesLong() {
		double value = Math.nextDown(0x1p63);
		JsonNode node = JsonNodeUtils.asNumericNode(jsonProvider, value);

		assertThat(node.isLong()).isTrue();
		assertThat(jsonProvider.asLong(node)).isEqualTo((long) value);
	}

	@Test
	void testDoubleAtLongLowerBoundaryBecomesLong() {
		JsonNode node = JsonNodeUtils.asNumericNode(jsonProvider, -0x1p63);

		assertThat(node.isLong()).isTrue();
		assertThat(jsonProvider.asLong(node)).isEqualTo(Long.MIN_VALUE);
	}

	@Test
	void testDoubleBelowLongLowerBoundaryRemainsDouble() {
		double value = Math.nextDown(-0x1p63);
		JsonNode node = JsonNodeUtils.asNumericNode(jsonProvider, value);

		assertThat(node.isDouble()).isTrue();
		assertThat(jsonProvider.asDouble(node)).isEqualTo(value);
	}
}
