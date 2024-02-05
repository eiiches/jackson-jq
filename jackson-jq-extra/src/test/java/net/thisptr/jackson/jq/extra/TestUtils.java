package net.thisptr.jackson.jq.extra;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.module.Module;
import net.thisptr.jackson.jq.module.ModuleLoader;

public class TestUtils {
	public static List<JsonNode> runQuery(String queryText, JsonNode in, Version version) throws JsonQueryException {
		Scope scope = Scope.newEmptyScope();
		scope.setModuleLoader(new ModuleLoaderForTest());
		JsonQuery query = JsonQuery.compile("import \"jackson-jq/extras\" as extras; " + queryText, version);
		List<JsonNode> results = new ArrayList<>();
		query.apply(scope, in, results::add);
		return results;
	}

	public static class ModuleLoaderForTest implements ModuleLoader {
		@Override
		public Module loadModule(Module caller, String path, JsonNode metadata) throws JsonQueryException {
			if (path.equals("jackson-jq/extras"))
				return new ModuleImpl();
			return null;
		}

		@Override
		public JsonNode loadData(Module caller, String path, JsonNode metadata) throws JsonQueryException {
			return null;
		}
	}
}
