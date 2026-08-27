package net.thisptr.jackson.jq.v2.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[1.0, 2.0.0.0]"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[01.0, 2.0]"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("1.0, 2.0"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[1.0]"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[2.0, 1.0]"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("(2.0, 1.0)"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("[1.0, 1.0)"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("(1.0, 1.0]"));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.valueOf("(1.0, 1.0)"));
		assertThrows(IllegalArgumentException.class, () -> new VersionRange(Version.valueOf("2.0"), true, Version.valueOf("1.0"), true));
		assertThrows(IllegalArgumentException.class, () -> new VersionRange(Version.valueOf("1.0"), true, Version.valueOf("1.0"), false));
		assertThrows(IllegalArgumentException.class, () -> new VersionRange(Version.valueOf("1.0"), false, Version.valueOf("1.0"), true));
		assertThrows(IllegalArgumentException.class, () -> new VersionRange(Version.valueOf("1.0"), false, Version.valueOf("1.0"), false));
	}

	@Test
	void testInclusivityNormalizationOnAbsentBounds() {
		assertEquals(VersionRange.valueOf("(, 1.5]"), VersionRange.valueOf("[, 1.5]"));
		assertEquals(VersionRange.valueOf("(, 1.5]").hashCode(), VersionRange.valueOf("[, 1.5]").hashCode());
		assertEquals(VersionRange.valueOf("[1.3, )"), VersionRange.valueOf("[1.3, ]"));
		assertEquals(VersionRange.valueOf("[1.3, )").hashCode(), VersionRange.valueOf("[1.3, ]").hashCode());
		assertEquals(VersionRange.valueOf("(,)"), VersionRange.valueOf("[,]"));
		assertEquals(VersionRange.valueOf("(,)").hashCode(), VersionRange.valueOf("[,]").hashCode());
		assertEquals(new VersionRange(null, false, Version.valueOf("1.5"), true), new VersionRange(null, true, Version.valueOf("1.5"), true));
		assertEquals(new VersionRange(Version.valueOf("1.3"), true, null, false), new VersionRange(Version.valueOf("1.3"), true, null, true));
		assertEquals(new VersionRange(null, false, null, false), new VersionRange(null, true, null, true));
	}

	@Test
	void testSinglePointRange() {
		VersionRange singlePoint = VersionRange.valueOf("[1.5, 1.5]");
		assertTrue(singlePoint.contains(Version.valueOf("1.5")));
		assertFalse(singlePoint.contains(Version.valueOf("1.4")));
		assertFalse(singlePoint.contains(Version.valueOf("1.6")));
	}

	@Test
	void testToString() {
		assertEquals("(,1.5.0]", VersionRange.valueOf("[, 1.5]").toString());
		assertEquals("[1.3.0,)", VersionRange.valueOf("[1.3, ]").toString());
		assertEquals("(,)", VersionRange.valueOf("[,]").toString());
		assertEquals("[1.0.0,2.0.0]", VersionRange.valueOf("[1.0, 2.0]").toString());
	}
}
