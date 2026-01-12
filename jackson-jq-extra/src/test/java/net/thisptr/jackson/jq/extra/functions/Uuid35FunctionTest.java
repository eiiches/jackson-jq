package net.thisptr.jackson.jq.extra.functions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;

import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.TestUtils;
import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Uuid35FunctionTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void testUuid3() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("extras::uuid3(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", JSON_PROVIDER.createString("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("9073926b-929f-31c2-abc9-fad77ae3e8eb"));
	}

	@Test
	public void testUuid5() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("extras::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", JSON_PROVIDER.createString("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}

	@Test
	public void testUuid5WithBinaryInput() throws JsonQueryException {
		// BinaryNode is Jackson 2 specific - tests are implementation-specific
		JsonNode in = BinaryNode.valueOf("example.com".getBytes(StandardCharsets.UTF_8));
		List<JsonNode> results = TestUtils.runQuery("extras::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", in, Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}
}
