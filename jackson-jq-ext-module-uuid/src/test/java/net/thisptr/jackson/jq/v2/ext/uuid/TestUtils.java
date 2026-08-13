package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class TestUtils {
	public static List<JsonNode> runQuery(String queryText, JsonNode in, Version version) throws JsonQueryException {
		Scope<JsonNode> scope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());
		scope.setModuleLoader(new ClassPathModuleLoader<>(TestUtils.class.getClassLoader()));
		JsonQuery query = JsonQuery.compile("import \"jackson-jq/uuid\" as uuid; " + queryText, version);
		List<JsonNode> results = new ArrayList<>();
		query.apply(scope, in, results::add);
		return results;
	}
}
