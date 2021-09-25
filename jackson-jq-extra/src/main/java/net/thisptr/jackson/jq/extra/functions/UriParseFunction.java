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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction("uriparse/0")
public class UriParseFunction implements Function {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Result {
		@JsonProperty("scheme")
		public String scheme;

		@JsonProperty("user_info")
		private String userInfo;
		@JsonProperty("raw_user_info")
		private String rawUserInfo;

		@JsonProperty("host")
		private String host;
		@JsonProperty("port")
		public int port;

		@JsonProperty("authority")
		private String authority;
		@JsonProperty("raw_authority")
		private String rawAuthority;

		@JsonProperty("path")
		private String path;
		@JsonProperty("raw_path")
		private String rawPath;

		@JsonProperty("query")
		private String query;
		@JsonProperty("raw_query")
		private String rawQuery;
		@JsonProperty("query_obj")
		private Map<String, JsonNode> queryObj;

		@JsonProperty("fragment")
		private String fragment;
		@JsonProperty("raw_fragment")
		private String rawFragment;

		public Result(final URI uri, final Map<String, JsonNode> queryObj) {
			this.port = uri.getPort();
			this.scheme = uri.getScheme();
			this.host = uri.getHost();
			this.path = uri.getPath();
			this.fragment = uri.getFragment();
			this.authority = uri.getAuthority();
			this.query = uri.getQuery();
			this.userInfo = uri.getUserInfo();
			this.rawQuery = uri.getRawQuery();
			this.queryObj = queryObj;
			this.rawUserInfo = uri.getRawUserInfo();
			this.rawAuthority = uri.getRawAuthority();
			this.rawFragment = uri.getRawFragment();
			this.rawPath = uri.getRawPath();
		}
	}

	private static final Pattern AMPERSAND = Pattern.compile(Pattern.quote("&"));
	private static final Pattern EQUAL = Pattern.compile(Pattern.quote("="));

	private Map<String, JsonNode> parseQueryObj(final Scope scope, final String rawQuery) {
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
				final ArrayNode arr = scope.getObjectMapper().createArrayNode();
				for (final String value : entry.getValue())
					arr.add(new TextNode(value));
				result2.put(entry.getKey(), arr);
			} else {
				result2.put(entry.getKey(), new TextNode(entry.getValue().get(0)));
			}
		}
		return result2;
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("uriparse", in, JsonNodeType.STRING);

		try {
			final URI uri = new URI(in.asText());
			final Result result = new Result(uri, parseQueryObj(scope, uri.getRawQuery()));
			output.emit(scope.getObjectMapper().valueToTree(result), null);
		} catch (URISyntaxException e) {
			throw new JsonQueryException(e);
		}
	}
}
