package net.thisptr.jackson.jq.v2.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NullLHSFunctionTest {
	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Environment<JsonNode> env = new EnvironmentBuilder<>(Jackson2JsonProviderImpl.getInstance(), Versions.JQ_1_5).build();
		ObjectNode input = mapper.createObjectNode().set("input", mapper.createArrayNode().add(1));
		List<JsonNode> output = new ArrayList<>();
		env.compile(".output+=[.input[0]+1]").apply(input, output::add);
		assertEquals(Arrays.asList(input.deepCopy().set("output", mapper.createArrayNode().add(2))), output);
	}
}
