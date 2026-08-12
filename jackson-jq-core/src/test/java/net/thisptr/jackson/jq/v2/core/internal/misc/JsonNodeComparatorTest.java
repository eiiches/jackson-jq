package net.thisptr.jackson.jq.v2.core.internal.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonNodeComparatorTest {

	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		JsonNode j3 = mapper.readTree("3");
		JsonNode j10 = mapper.readTree("10");
		JsonNode jhoge = mapper.readTree("\"hoge\"");

		JsonNodeComparator<JsonNode> sut = new JsonNodeComparator<>(Jackson2JsonProviderImpl.getInstance());
		assertTrue(sut.compare(j3, j10) < 0);
		assertTrue(sut.compare(j3, jhoge) < 0);
		assertTrue(sut.compare(j10, jhoge) < 0);

		List<JsonNode> nodes = new ArrayList<>(Arrays.asList(j3, jhoge, j10));
		nodes.sort(sut);
		assertEquals(Arrays.asList(j3, j10, jhoge), nodes);
	}
}
