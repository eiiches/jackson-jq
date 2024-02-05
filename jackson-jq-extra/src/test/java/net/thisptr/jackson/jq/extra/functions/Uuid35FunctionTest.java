package net.thisptr.jackson.jq.extra.functions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.TestUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Uuid35FunctionTest {

	@Test
	public void testUuid3() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("extras::uuid3(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", TextNode.valueOf("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(TextNode.valueOf("9073926b-929f-31c2-abc9-fad77ae3e8eb"));
	}

	@Test
	public void testUuid5() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("extras::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", TextNode.valueOf("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(TextNode.valueOf("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}

	@Test
	public void testUuid5WithBinaryInput() throws JsonQueryException {
		JsonNode in = BinaryNode.valueOf("example.com".getBytes(StandardCharsets.UTF_8));
		List<JsonNode> results = TestUtils.runQuery("extras::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", in, Versions.JQ_1_6);
		assertThat(results).containsExactly(TextNode.valueOf("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}
}
