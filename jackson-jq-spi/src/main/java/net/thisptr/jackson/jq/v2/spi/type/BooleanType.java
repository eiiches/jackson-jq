package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The boolean type.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class BooleanType implements Type {
	private static final BooleanType INSTANCE = new BooleanType();

	/**
	 * Returns the singleton instance of {@link BooleanType}.
	 *
	 * @return the singleton instance
	 */
	public static BooleanType getInstance() {
		return INSTANCE;
	}

	private BooleanType() {
	}

	@Override
	public String toString() {
		return "BOOLEAN";
	}
}
