package net.thisptr.jackson.jq.extra.functions;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@SuppressWarnings("rawtypes")
@AutoService(Function.class)
@BuiltinFunction("uriparse/0")
public class UriParseFunction<JsonNode> implements Function<JsonNode> {

	private static final Pattern AMPERSAND = Pattern.compile(Pattern.quote("&"));
	private static final Pattern EQUAL = Pattern.compile(Pattern.quote("="));

	private Map<String, JsonNode> parseQueryObj(final Scope<JsonNode> scope, final String rawQuery) {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		final Map<String, List<String>> result = new HashMap<>();
		if (rawQuery == null)
			return Collections.emptyMap();
		for (final String kv : AMPERSAND.split(rawQuery, -1)) {
			final String[] tuple = EQUAL.split(kv, -1);
			if (tuple.length != 2)
				continue;
			final String keyEncoded = tuple[0];
			final String valueEncoded = tuple[1];

			try {
				final String key = URLDecoder.decode(keyEncoded, "UTF-8");
				final String value = URLDecoder.decode(valueEncoded, "UTF-8");
				List<String> arr = result.get(key);
				if (arr == null) {
					arr = new ArrayList<>(1);
					result.put(key, arr);
				}
				arr.add(value);
			} catch (Exception e) {
				continue;
			}
		}
		final Map<String, JsonNode> result2 = new HashMap<>();
		for (final Map.Entry<String, List<String>> entry : result.entrySet()) {
			if (entry.getValue().size() > 1) {
				JsonNode arr = jsonProvider.createArray();
				for (final String value : entry.getValue())
					arr = jsonProvider.add(arr, jsonProvider.createString(value));
				result2.put(entry.getKey(), arr);
			} else {
				result2.put(entry.getKey(), jsonProvider.createString(entry.getValue().get(0)));
			}
		}
		return result2;
	}

	private JsonNode buildResult(final JsonProvider<JsonNode> jsonProvider, final URI uri, final Map<String, JsonNode> queryObj) {
		JsonNode result = jsonProvider.createObject();
		result = jsonProvider.set(result, "scheme", uri.getScheme() != null ? jsonProvider.createString(uri.getScheme()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "user_info", uri.getUserInfo() != null ? jsonProvider.createString(uri.getUserInfo()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_user_info", uri.getRawUserInfo() != null ? jsonProvider.createString(uri.getRawUserInfo()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "host", uri.getHost() != null ? jsonProvider.createString(uri.getHost()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "port", jsonProvider.createInt(uri.getPort()));
		result = jsonProvider.set(result, "authority", uri.getAuthority() != null ? jsonProvider.createString(uri.getAuthority()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_authority", uri.getRawAuthority() != null ? jsonProvider.createString(uri.getRawAuthority()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "path", uri.getPath() != null ? jsonProvider.createString(uri.getPath()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_path", uri.getRawPath() != null ? jsonProvider.createString(uri.getRawPath()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "query", uri.getQuery() != null ? jsonProvider.createString(uri.getQuery()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_query", uri.getRawQuery() != null ? jsonProvider.createString(uri.getRawQuery()) : jsonProvider.createNull());

		JsonNode queryObjNode = jsonProvider.createObject();
		for (final Map.Entry<String, JsonNode> entry : queryObj.entrySet()) {
			queryObjNode = jsonProvider.set(queryObjNode, entry.getKey(), entry.getValue());
		}
		result = jsonProvider.set(result, "query_obj", queryObjNode);

		result = jsonProvider.set(result, "fragment", uri.getFragment() != null ? jsonProvider.createString(uri.getFragment()) : jsonProvider.createNull());
		result = jsonProvider.set(result, "raw_fragment", uri.getRawFragment() != null ? jsonProvider.createString(uri.getRawFragment()) : jsonProvider.createNull());
		return result;
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "uriparse", in, JsonNodeType.STRING);

		try {
			final URI uri = new URI(jsonProvider.asText(in));
			final Map<String, JsonNode> queryObj = parseQueryObj(scope, uri.getRawQuery());
			output.emit(buildResult(jsonProvider, uri, queryObj), null);
		} catch (URISyntaxException e) {
			throw new JsonQueryException(e);
		}
	}
}
