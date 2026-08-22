package net.thisptr.jackson.jq.v2.smoketests.quarkus;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusMainTest
public class QuarkusSmokeTestMainTest {
	@Test
	@Launch({})
	public void testSmokeTest(final LaunchResult result) {
		assertEquals(0, result.exitCode());
		assertTrue(result.getOutput().contains("Quarkus compatibility test passed for Jackson 2, Jackson 3, Gson, and all extension modules"));
	}
}
