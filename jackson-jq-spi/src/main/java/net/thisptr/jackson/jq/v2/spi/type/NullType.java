package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The null type.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class NullType implements Type {
	private static final NullType INSTANCE = new NullType();

	/**
	 * Returns the singleton instance of {@link NullType}.
	 *
	 * @return the singleton instance
	 */
	public static NullType getInstance() {
		return INSTANCE;
	}

	private NullType() {
	}

	@Override
	public String toString() {
		return "NULL";
	}
}
