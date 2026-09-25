package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The type of an expression that emits no values.
 * <p>
 * This is the bottom type and the identity element of a union.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class NeverType implements Type {
	private static final NeverType INSTANCE = new NeverType();

	/**
	 * Returns the singleton instance of {@link NeverType}.
	 *
	 * @return the singleton instance
	 */
	public static NeverType getInstance() {
		return INSTANCE;
	}

	private NeverType() {
	}

	@Override
	public String toString() {
		return "NEVER";
	}
}
