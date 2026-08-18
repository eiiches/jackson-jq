package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class UriParseFunction implements Function {

	private static final Pattern AMPERSAND = Pattern.compile(Pattern.quote("&"));
	private static final Pattern EQUAL = Pattern.compile(Pattern.quote("="));

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		return (frame, in, ipath, output, ignoredRequirePath) -> {
			Preconditions.checkInputType(jsonProvider, "uriparse", in, JsonNodeType.STRING);

			try {
				URI uri = new URI(jsonProvider.asText(in));
				Map<String, JsonNode> queryObj = parseQueryObj(jsonProvider, uri.getRawQuery());
				output.emit(buildResult(jsonProvider, uri, queryObj), null);
			} catch (URISyntaxException e) {
				throw new JsonQueryException(e);
			}
		};
	}

	private <JsonNode> Map<String, JsonNode> parseQueryObj(JsonProvider<JsonNode> jsonProvider, String rawQuery) {
		Map<String, List<String>> result = new HashMap<>();
		if (rawQuery == null)
			return Collections.emptyMap();
		for (String kv : AMPERSAND.split(rawQuery, -1)) {
			String[] tuple = EQUAL.split(kv, -1);
			if (tuple.length != 2)
				continue;
			String keyEncoded = tuple[0];
			String valueEncoded = tuple[1];

			try {
				String key = URLDecoder.decode(keyEncoded, "UTF-8");
				String value = URLDecoder.decode(valueEncoded, "UTF-8");
				@Var List<String> arr = result.get(key);
				if (arr == null) {
					arr = new ArrayList<>(1);
					result.put(key, arr);
				}
				arr.add(value);
			} catch (Exception e) {
				continue;
			}
		}
		Map<String, JsonNode> result2 = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : result.entrySet()) {
			if (entry.getValue().size() > 1) {
				@Var JsonNode arr = jsonProvider.createArray();
				for (String value : entry.getValue())
					arr = jsonProvider.add(arr, jsonProvider.createString(value));
				result2.put(entry.getKey(), arr);
			} else {
				result2.put(entry.getKey(), jsonProvider.createString(entry.getValue().get(0)));
			}
		}
		return result2;
	}

	private <JsonNode> JsonNode buildResult(JsonProvider<JsonNode> jsonProvider, URI uri, Map<String, JsonNode> queryObj) {
		@Var JsonNode result = jsonProvider.createObject();
		result = jsonProvider.set(result, "scheme", uri.getScheme() != null ? jsonProvider.createString(uri.getScheme()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "user_info", uri.getUserInfo() != null ? jsonProvider.createString(uri.getUserInfo()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_user_info", uri.getRawUserInfo() != null ? jsonProvider.createString(uri.getRawUserInfo()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "host", uri.getHost() != null ? jsonProvider.createString(uri.getHost()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "port", jsonProvider.createNumber(uri.getPort()));
		result = jsonProvider.set(result, "authority", uri.getAuthority() != null ? jsonProvider.createString(uri.getAuthority()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_authority", uri.getRawAuthority() != null ? jsonProvider.createString(uri.getRawAuthority()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "path", uri.getPath() != null ? jsonProvider.createString(uri.getPath()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_path", uri.getRawPath() != null ? jsonProvider.createString(uri.getRawPath()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "query", uri.getQuery() != null ? jsonProvider.createString(uri.getQuery()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_query", uri.getRawQuery() != null ? jsonProvider.createString(uri.getRawQuery()) : jsonProvider.createNull());

		@Var JsonNode queryObjNode = jsonProvider.createObject();
		for (Map.Entry<String, JsonNode> entry : queryObj.entrySet()) {
			queryObjNode = jsonProvider.set(queryObjNode, entry.getKey(), entry.getValue());
		}
		result = jsonProvider.set(result, "query_obj", queryObjNode);

		result = jsonProvider.set(result, "fragment", uri.getFragment() != null ? jsonProvider.createString(uri.getFragment()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_fragment", uri.getRawFragment() != null ? jsonProvider.createString(uri.getRawFragment()) : jsonProvider.createNull());
		return result;
	}
}
