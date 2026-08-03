package net.thisptr.jackson.jq;

import java.util.Arrays;
import java.util.List;

public class Versions {

	public static final Version JQ_1_5 = new Version(1, 5);

	public static final Version JQ_1_6 = new Version(1, 6);

	public static final Version JQ_1_7 = new Version(1, 7);

	public static List<Version> versions() {
		return Arrays.asList(JQ_1_5, JQ_1_6, JQ_1_7);
	}

	private Versions() {}
}
