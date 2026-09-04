package net.thisptr.jackson.jq.v2.ext.uri.functions;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.ext.uri.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class UriParseFunction implements Function {
	private static final Pattern AMPERSAND = Pattern.compile(Pattern.quote("&"));
	private static final Pattern EQUAL = Pattern.compile(Pattern.quote("="));

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				return Cardinality.ONE;
			}

			@Override
			public boolean dependsOnExternalState() {
				return false;
			}

			@Override
			public void apply(Context context, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
				Preconditions.checkInputType(jsonProvider, "uriparse", in, JsonNodeType.STRING);
				try {
					URI uri = new URI(jsonProvider.getString(in));
					Map<String, JsonNode> queryObj = parseQueryObj(jsonProvider, uri.getRawQuery());
					output.emit(buildResult(jsonProvider, uri, queryObj), UntrackedPath.getInstance());
				} catch (URISyntaxException e) {
					throw new JsonQueryException(e);
				}
			}
		};
	}

	// Suppress JdkObsolete because URLDecoder.decode(String, Charset) is not available in Java 8 target.
	@SuppressWarnings("JdkObsolete")
	private <JsonNode> Map<String, JsonNode> parseQueryObj(JsonProvider<JsonNode> jsonProvider, String rawQuery) {
		if (rawQuery == null)
			return new HashMap<>();
		Map<String, List<String>> result = new HashMap<>();
		for (String kv : AMPERSAND.split(rawQuery, -1)) {
			String[] tuple = EQUAL.split(kv, -1);
			if (tuple.length != 2)
				continue;
			String keyEncoded = tuple[0];
			String valueEncoded = tuple[1];
			try {
				String key = URLDecoder.decode(keyEncoded, StandardCharsets.UTF_8.name());
				String value = URLDecoder.decode(valueEncoded, StandardCharsets.UTF_8.name());
				@Var List<String> arr = result.get(key);
				if (arr == null) {
					arr = new ArrayList<>(1);
					result.put(key, arr);
				}
				arr.add(value);
			} catch (Exception e) {
				// ignore malformed query parameters
			}
		}
		Map<String, JsonNode> result2 = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : result.entrySet()) {
			if (entry.getValue().size() > 1) {
				List<JsonNode> arr = new ArrayList<>(entry.getValue().size());
				for (String value : entry.getValue())
					arr.add(jsonProvider.createString(value));
				result2.put(entry.getKey(), jsonProvider.createArray(arr));
			} else {
				result2.put(entry.getKey(), jsonProvider.createString(entry.getValue().get(0)));
			}
		}
		return result2;
	}

	private <JsonNode> JsonNode buildResult(JsonProvider<JsonNode> jsonProvider, URI uri, Map<String, JsonNode> queryObj) {
		Map<String, JsonNode> result = new LinkedHashMap<>();
		result.put("scheme", uri.getScheme() != null ? jsonProvider.createString(uri.getScheme()) : jsonProvider.createNull());
		result.put("user_info", uri.getUserInfo() != null ? jsonProvider.createString(uri.getUserInfo()) : jsonProvider.createNull());
		result.put("raw_user_info", uri.getRawUserInfo() != null ? jsonProvider.createString(uri.getRawUserInfo()) : jsonProvider.createNull());
		result.put("host", uri.getHost() != null ? jsonProvider.createString(uri.getHost()) : jsonProvider.createNull());
		result.put("port", jsonProvider.createNumber(uri.getPort()));
		result.put("authority", uri.getAuthority() != null ? jsonProvider.createString(uri.getAuthority()) : jsonProvider.createNull());
		result.put("raw_authority", uri.getRawAuthority() != null ? jsonProvider.createString(uri.getRawAuthority()) : jsonProvider.createNull());
		result.put("path", uri.getPath() != null ? jsonProvider.createString(uri.getPath()) : jsonProvider.createNull());
		result.put("raw_path", uri.getRawPath() != null ? jsonProvider.createString(uri.getRawPath()) : jsonProvider.createNull());
		result.put("query", uri.getQuery() != null ? jsonProvider.createString(uri.getQuery()) : jsonProvider.createNull());
		result.put("raw_query", uri.getRawQuery() != null ? jsonProvider.createString(uri.getRawQuery()) : jsonProvider.createNull());
		result.put("query_obj", jsonProvider.createObject(queryObj));
		result.put("fragment", uri.getFragment() != null ? jsonProvider.createString(uri.getFragment()) : jsonProvider.createNull());
		result.put("raw_fragment", uri.getRawFragment() != null ? jsonProvider.createString(uri.getRawFragment()) : jsonProvider.createNull());
		return jsonProvider.createObject(result);
	}
}
