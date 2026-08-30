package net.thisptr.jackson.jq.v2.ext.uuid.functions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.ext.uuid.TestUtils;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;

public class Uuid35FunctionTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	public void testUuid4() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("uuid::uuid4", JSON_PROVIDER.createNull(), Versions.JQ_1_6);
		assertThat(JSON_PROVIDER.asString(results.get(0))).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
	}

	@Test
	public void testUuid3() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("uuid::uuid3(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", JSON_PROVIDER.createString("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("9073926b-929f-31c2-abc9-fad77ae3e8eb"));
	}

	@Test
	public void testUuid5() throws JsonQueryException {
		List<JsonNode> results = TestUtils.runQuery("uuid::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", JSON_PROVIDER.createString("example.com"), Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}

	@Test
	public void testUuid5WithBinaryInput() throws JsonQueryException {
		// BinaryNode is Jackson 2 specific - tests are implementation-specific
		JsonNode in = BinaryNode.valueOf("example.com".getBytes(StandardCharsets.UTF_8));
		List<JsonNode> results = TestUtils.runQuery("uuid::uuid5(\"6ba7b810-9dad-11d1-80b4-00c04fd430c8\")", in, Versions.JQ_1_6);
		assertThat(results).containsExactly(JSON_PROVIDER.createString("cfbff0d1-9375-5685-968c-48ce8b15ae17"));
	}
}
