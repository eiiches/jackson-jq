package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The binary value type. Binary is not a JSON type; only providers whose underlying library has
 * such a node ever produce one.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class BinaryType implements Type {
	private static final BinaryType INSTANCE = new BinaryType();

	/**
	 * Returns the singleton instance of {@link BinaryType}.
	 *
	 * @return the singleton instance
	 */
	public static BinaryType getInstance() {
		return INSTANCE;
	}

	private BinaryType() {
	}

	@Override
	public String toString() {
		return "BINARY";
	}
}
