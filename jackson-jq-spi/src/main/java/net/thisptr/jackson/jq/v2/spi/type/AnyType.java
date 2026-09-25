package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The type that accepts every defined JSON value. This does not include {@link UndefinedType}.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class AnyType implements Type {
	private static final AnyType INSTANCE = new AnyType();

	/**
	 * Returns the singleton instance of {@link AnyType}.
	 *
	 * @return the singleton instance
	 */
	public static AnyType getInstance() {
		return INSTANCE;
	}

	private AnyType() {
	}

	@Override
	public String toString() {
		return "ANY";
	}
}
