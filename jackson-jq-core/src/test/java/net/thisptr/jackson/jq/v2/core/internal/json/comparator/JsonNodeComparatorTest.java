package net.thisptr.jackson.jq.v2.core.internal.json.comparator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonNodeComparatorTest {

	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		JsonNode j3 = mapper.readTree("3");
		JsonNode j10 = mapper.readTree("10");
		JsonNode jhoge = mapper.readTree("\"hoge\"");

		JsonNodeComparator<JsonNode> sut = new JsonNodeComparator<>(Jackson2JsonProvider.getInstance());
		assertThat(sut.compare(j3, j10)).isLessThan(0);
		assertThat(sut.compare(j3, jhoge)).isLessThan(0);
		assertThat(sut.compare(j10, jhoge)).isLessThan(0);

		List<JsonNode> nodes = new ArrayList<>(List.of(j3, jhoge, j10));
		nodes.sort(sut);
		assertThat(nodes).isEqualTo(List.of(j3, j10, jhoge));
	}

	/**
	 * A binary node is not a JSON value, so it can only reach the comparator as caller-supplied
	 * input. It has an order class of its own, after every string, and is compared byte by byte.
	 */
	@Test
	public void testBinary() {
		JsonNode binary = BinaryNode.valueOf(new byte[] { 1, 2 });
		JsonNode binaryLonger = BinaryNode.valueOf(new byte[] { 1, 2, 3 });
		JsonNode binaryHigh = BinaryNode.valueOf(new byte[] { -1 });
		JsonNode j3 = IntNode.valueOf(3);
		JsonNode jhoge = TextNode.valueOf("hoge");

		JsonNodeComparator<JsonNode> sut = new JsonNodeComparator<>(Jackson2JsonProvider.getInstance());
		assertThat(sut.compare(binary, BinaryNode.valueOf(new byte[] { 1, 2 }))).isZero();
		assertThat(sut.compare(binary, binaryLonger)).isLessThan(0);
		// 0xff sorts after 0x01, i.e. the bytes are compared unsigned.
		assertThat(sut.compare(binary, binaryHigh)).isLessThan(0);
		assertThat(sut.compare(j3, binary)).isLessThan(0);
		assertThat(sut.compare(jhoge, binary)).isLessThan(0);
		assertThat(sut.compare(binary, jhoge)).isGreaterThan(0);

		List<JsonNode> nodes = new ArrayList<>(List.of(binaryHigh, jhoge, binary, j3));
		nodes.sort(sut);
		assertThat(nodes).isEqualTo(List.of(j3, jhoge, binary, binaryHigh));
	}
}
