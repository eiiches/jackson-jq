package net.thisptr.jackson.jq.v2.ext.joni;

import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.NullType;
import net.thisptr.jackson.jq.v2.spi.type.NumberKind;
import net.thisptr.jackson.jq.v2.spi.type.NumericType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * The shapes the regex primitives and the jq definitions written on top of them both speak in. They
 * live here so that a definition's published signature cannot drift from the primitive it calls.
 */
final class Types {
	/**
	 * The text a group matched, or nothing when it did not participate in the match.
	 */
	static final Type STRING_OR_NULL = UnionType.of(StringType.getInstance(), NullType.getInstance());

	/**
	 * A group that did not participate in the match has no text and no offset.
	 */
	static final Type CAPTURE = ObjectType.of(
			"offset", NumericType.of(NumberKind.INT),
			"length", NumericType.of(NumberKind.INT),
			"string", STRING_OR_NULL,
			"name", STRING_OR_NULL
	);

	/**
	 * A match's own {@code string} is the text it matched, so unlike a capture's it is never null.
	 */
	static final Type MATCH_OBJECT = ObjectType.of(
			"offset", NumericType.of(NumberKind.INT),
			"length", NumericType.of(NumberKind.INT),
			"string", StringType.getInstance(),
			"captures", ArrayType.of(CAPTURE)
	);

	/**
	 * The object keyed by capture name that {@code capture} builds, and that {@code _sub_impl} hands its
	 * replacement filter.
	 */
	static final Type CAPTURES = ObjectType.of(STRING_OR_NULL);

	private Types() {
	}
}
