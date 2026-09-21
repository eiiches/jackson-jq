package net.thisptr.jackson.jq.v2.spi.type;

/**
 * The absence of a value. A declared object field whose type includes this type is optional.
 * <p>
 * This type has a single instance, available via {@link #getInstance()}.
 */
public final class UndefinedType implements Type {
	private static final UndefinedType INSTANCE = new UndefinedType();

	/**
	 * Returns the singleton instance of {@link UndefinedType}.
	 *
	 * @return the singleton instance
	 */
	public static UndefinedType getInstance() {
		return INSTANCE;
	}

	private UndefinedType() {
	}

	@Override
	public String toString() {
		return "UNDEFINED";
	}
}
