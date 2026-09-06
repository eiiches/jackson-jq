package net.thisptr.jackson.jq.v2.spi.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VersionTest {

	@Test
	void testValidVersions() {
		Version v1 = Version.valueOf("1.0");
		assertEquals(1, v1.major());
		assertEquals(0, v1.minor());
		assertEquals(0, v1.patch());

		Version v2 = Version.valueOf("1.2.3");
		assertEquals(1, v2.major());
		assertEquals(2, v2.minor());
		assertEquals(3, v2.patch());

		Version v3 = Version.valueOf("0.1.0");
		assertEquals(0, v3.major());
		assertEquals(1, v3.minor());
		assertEquals(0, v3.patch());

		Version v4 = Version.valueOf("10.20.30");
		assertEquals(10, v4.major());
		assertEquals(20, v4.minor());
		assertEquals(30, v4.patch());
	}

	@Test
	void testOfComponents() {
		Version v1 = Version.of(1, 2, 3);
		assertEquals(1, v1.major());
		assertEquals(2, v1.minor());
		assertEquals(3, v1.patch());

		Version v2 = Version.of(1, 2);
		assertEquals(1, v2.major());
		assertEquals(2, v2.minor());
		assertEquals(0, v2.patch());
	}

	@Test
	void testInvalidVersions() {
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0.0"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("01.0.0"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.0-SNAPSHOT"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf(""));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("abc"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("-1.0"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.-1.0"));
		assertThrows(IllegalArgumentException.class, () -> Version.valueOf("1.0.-1"));
		assertThrows(IllegalArgumentException.class, () -> Version.of(-1, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> Version.of(0, -1, 0));
		assertThrows(IllegalArgumentException.class, () -> Version.of(0, 0, -1));
		assertThrows(IllegalArgumentException.class, () -> Version.of(-1, -1, -1));
		assertThrows(IllegalArgumentException.class, () -> Version.of(-1, 0));
		assertThrows(IllegalArgumentException.class, () -> Version.of(0, -1));
	}
}
