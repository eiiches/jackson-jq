package net.thisptr.jackson.jq.v2.test.providers.gson;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;
import net.thisptr.jackson.jq.v2.json.internal.io.ParseOptions;
import net.thisptr.jackson.jq.v2.test.AbstractJsonQueryTest;

/**
 * Concrete implementation of AbstractJsonQueryTest for Gson.
 * Runs the standard jq test suite using GsonJsonProvider.
 */
public class GsonJsonQueryTest extends AbstractJsonQueryTest<JsonElement> {
	public static void main(String[] args) throws Exception {
		new GsonJsonQueryTest().run(args);
	}

	@Override
	protected JsonProvider<JsonElement> getJsonProvider() {
		return GsonJsonProvider.getInstance();
	}

	@Override
	protected JsonElement parseTestNode(JsonNode node, ParseOptions parseOptions) {
		// Read it the way the library itself would, so the jq version's number handling applies
		return JsonCodec.parse(getJsonProvider(), node.toString(), parseOptions);
	}
}
