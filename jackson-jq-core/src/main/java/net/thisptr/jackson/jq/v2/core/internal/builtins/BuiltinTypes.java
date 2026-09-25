package net.thisptr.jackson.jq.v2.core.internal.builtins;

import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Types that appear in the signature of more than one builtin.
 */
final class BuiltinTypes {
	/**
	 * A location within a JSON document: the field names and array indices leading to it, as
	 * {@code path/1} and {@code paths/1} produce them and {@code getpath/1}, {@code setpath/2} and
	 * {@code delpaths/1} consume them.
	 */
	static final Type PATH = ArrayType.of(UnionType.of(StringType.getInstance(), NumericType.getInstance()));

	private BuiltinTypes() {
	}
}
