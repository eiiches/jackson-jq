package net.thisptr.jackson.jq.v2.core;

import java.util.Arrays;
import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Version;

public class Versions {

	public static final Version JQ_1_5 = Version.valueOf("1.5");

	public static final Version JQ_1_6 = Version.valueOf("1.6");

	public static final Version JQ_1_7 = Version.valueOf("1.7");

	public static final Version JQ_1_7_1 = Version.valueOf("1.7.1");

	public static List<Version> versions() {
		return Arrays.asList(JQ_1_5, JQ_1_6, JQ_1_7, JQ_1_7_1);
	}

	private Versions() {}
}
