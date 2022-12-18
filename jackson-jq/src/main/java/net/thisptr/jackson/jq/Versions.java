package net.thisptr.jackson.jq;

import java.util.Arrays;
import java.util.List;

public class Versions {

	public static final Version JQ_1_5 = new Version(1, 5);

	public static final Version JQ_1_6 = new Version(1, 6);

	public static final Version LATEST = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE);

	public static List<Version> versions() {
		return Arrays.asList(JQ_1_5, JQ_1_6, LATEST);
	}

	private Versions() {}
}
