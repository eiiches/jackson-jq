package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The string type.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class StringType implements Type {
	private static final StringType INSTANCE = new StringType();

	/**
	 * Returns the singleton instance of {@link StringType}.
	 *
	 * @return the singleton instance
	 */
	public static StringType getInstance() {
		return INSTANCE;
	}

	private StringType() {
	}

	@Override
	public String toString() {
		return "STRING";
	}
}
