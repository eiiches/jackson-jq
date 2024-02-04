package net.thisptr.jackson.jq.extra.internal.misc;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UuidUtils {

	public static UUID uuid3(final UUID namespace, final byte[] name) {
		return uuid3or5(namespace, name, 3);
	}

	public static UUID uuid5(final UUID namespace, final byte[] name) {
		return uuid3or5(namespace, name, 5);
	}

	public static UUID uuid3or5(final UUID namespace, final byte[] name, final int version) {
		// https://datatracker.ietf.org/doc/html/rfc4122#section-4.3
		MessageDigest md;
		try {
			switch (version) {
				case 3:
					md = MessageDigest.getInstance("MD5");
					break;
				case 5:
					md = MessageDigest.getInstance("SHA-1");
					break;
				default:
					throw new IllegalArgumentException("invalid version");
			}
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		md.update(toBytes(namespace));
		md.update(name);
		byte[] hash = md.digest();

		// put in the variant and version bits
		hash[6] &= (byte) 0b0000_1111;
		hash[6] |= (byte) (version << 4);
		hash[8] &= (byte) 0b0011_1111;
		hash[8] |= (byte) 0b1000_0000;
		return fromBytes(hash);
	}

	public static byte[] toBytes(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	public static UUID fromBytes(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		long mostSigBits = bb.getLong();
		long leastSigBits = bb.getLong();
		return new UUID(mostSigBits, leastSigBits);
	}
}
