package net.thisptr.jackson.jq.v2.spi.annotations;

public @interface VersionRangeSpec {
	VersionSpec min() default @VersionSpec(major = 0, minor = 0, patch = 0);

	boolean minInclusive() default true;

	VersionSpec max() default @VersionSpec(major = Integer.MAX_VALUE, minor = Integer.MAX_VALUE, patch = Integer.MAX_VALUE);

	boolean maxInclusive() default false;
}
