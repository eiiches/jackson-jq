package net.thisptr.jackson.jq.extra.internal.misc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidUtilsTest {

	private static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

	@Test
	public void testUuid3() {
		// https://www.uuidtools.com/v3
		assertThat(UuidUtils.uuid3(NAMESPACE_DNS, "example.com".getBytes(StandardCharsets.UTF_8)))
				.isEqualTo(UUID.fromString("9073926b-929f-31c2-abc9-fad77ae3e8eb"));
	}

	@Test
	public void testUuid5() {
		// https://www.uuidtools.com/v5
		assertThat(UuidUtils.uuid5(NAMESPACE_DNS, "example.com".getBytes(StandardCharsets.UTF_8)))
				.isEqualTo(UUID.fromString("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}

	@Test
	public void testToBytesAndFromBytes() {
		byte[] namespaceDnsBytes = new byte[]{
				0x6b, (byte) 0xa7, (byte) 0xb8, 0x10,
				(byte) 0x9d, (byte) 0xad,
				0x11, (byte) 0xd1,
				(byte) 0x80, (byte) 0xb4,
				0x00, (byte) 0xc0, 0x4f, (byte) 0xd4, 0x30, (byte) 0xc8,
		};
		assertThat(UuidUtils.toBytes(NAMESPACE_DNS)).isEqualTo(namespaceDnsBytes);
		assertThat(UuidUtils.fromBytes(namespaceDnsBytes)).isEqualTo(NAMESPACE_DNS);
	}
}
