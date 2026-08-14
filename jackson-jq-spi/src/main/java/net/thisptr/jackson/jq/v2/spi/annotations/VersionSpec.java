package net.thisptr.jackson.jq.v2.spi.annotations;

public @interface VersionSpec {
	int major();

	int minor();

	int patch();
}
