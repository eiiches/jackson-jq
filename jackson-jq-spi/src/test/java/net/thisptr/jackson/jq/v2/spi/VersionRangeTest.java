package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionRangeTest {

	@Test
	void testMaxBounds() throws Exception {
		assertTrue(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.5")));
		assertFalse(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.6")));
		assertTrue(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.4")));
		assertFalse(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.5")));
		assertFalse(VersionRange.valueOf("[1.3, 1.5)").contains(Version.valueOf("1.6")));
		assertFalse(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.2")));
		assertTrue(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.3")));
		assertTrue(VersionRange.valueOf("[1.3,)").contains(Version.valueOf("1.4")));
		assertTrue(VersionRange.valueOf("[,]").contains(Version.valueOf("1.0")));
	}

	@Test
	void testMinBounds() throws Exception {
		assertFalse(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.2")));
		assertTrue(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.3")));
		assertTrue(VersionRange.valueOf("[1.3, 1.5]").contains(Version.valueOf("1.4")));

		assertFalse(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.2")));
		assertFalse(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.3")));
		assertTrue(VersionRange.valueOf("(1.3, 1.5]").contains(Version.valueOf("1.4")));

		assertTrue(VersionRange.valueOf("[, 1.5]").contains(Version.valueOf("1.0")));
		assertTrue(VersionRange.valueOf("(, 1.5]").contains(Version.valueOf("1.0")));
		assertFalse(VersionRange.valueOf("[, 1.5]").contains(Version.valueOf("1.6")));
		assertFalse(VersionRange.valueOf("(, 1.5]").contains(Version.valueOf("1.6")));
	}

	@Test
	void testMultiDigitAndThreeParts() {
		assertTrue(VersionRange.valueOf("[1.0.0, 2.3.4)").contains(Version.valueOf("2.3.3")));
		assertFalse(VersionRange.valueOf("[1.0.0, 2.3.4)").contains(Version.valueOf("2.3.4")));
		assertTrue(VersionRange.valueOf("[10.20.30, 40.50.60]").contains(Version.valueOf("10.20.30")));
		assertTrue(VersionRange.valueOf("[10.20.30, 40.50.60]").contains(Version.valueOf("40.50.60")));
	}

	@Test
	void testInvalidRanges() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[1.0, 2.0.0.0]"));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[01.0, 2.0]"));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("1.0, 2.0"));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[1.0]"));
	}
}
