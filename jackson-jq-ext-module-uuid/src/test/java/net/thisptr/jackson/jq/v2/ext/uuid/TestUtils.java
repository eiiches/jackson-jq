package net.thisptr.jackson.jq.v2.ext.uuid;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.module.loaders.ClassPathModuleLoader;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class TestUtils {
	public static List<JsonNode> runQuery(String queryText, JsonNode in, Version version) throws JsonQueryException {
		Environment<JsonNode> env = new Environment<>(Jackson2JsonProviderImpl.getInstance(), version);
		env.setModuleLoader(new ClassPathModuleLoader<>(TestUtils.class.getClassLoader()));
		JsonQuery<JsonNode> query = env.compile("import \"jackson-jq/uuid\" as uuid; " + queryText);
		List<JsonNode> results = new ArrayList<>();
		query.apply(in, (val, path) -> results.add(val));
		return results;
	}
}
