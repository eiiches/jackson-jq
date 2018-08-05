package net.thisptr.jackson.jq.internal.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.thisptr.jackson.jq.DefaultRootScope;
import net.thisptr.jackson.jq.JsonQuery;

public class PipedQueryTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void test1() throws Exception {
		final JsonQuery q = JsonQuery.compile("label $out | [foreach .[] as $item ([3, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1])]");
		assertEquals(Collections.emptyList(), q.apply(DefaultRootScope.getInstance(), MAPPER.readTree("[11,22,33,44,55,66,77,88,99]")));
	}

	@Test
	void test2() throws Exception {
		final JsonQuery q = JsonQuery.compile("[label $out | foreach .[] as $item ([3, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1])]");
		assertEquals(Collections.singletonList(MAPPER.readTree("[11,22,33]")), q.apply(DefaultRootScope.getInstance(), MAPPER.readTree("[11,22,33,44,55,66,77,88,99]")));
	}

	@Test
	void test3() throws Exception {
		final JsonQuery q = JsonQuery.compile("[foreach .[] as $item ([3, null]; label $out | if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1])]");
		assertEquals(Collections.singletonList(MAPPER.readTree("[11,22,33]")), q.apply(DefaultRootScope.getInstance(), MAPPER.readTree("[11,22,33,44,55,66,77,88,99]")));
	}
}
