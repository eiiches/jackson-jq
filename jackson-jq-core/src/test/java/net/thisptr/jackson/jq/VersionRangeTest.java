package net.thisptr.jackson.jq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
