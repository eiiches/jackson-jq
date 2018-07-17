package net.thisptr.jackson.jq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonQueryUtils {
	private JsonQueryUtils() {}

	@Deprecated
	private static Scope defaultScope = new Scope();

	@Deprecated
	public static <T> List<T> apply(final JsonQuery jq, final Object in, final Class<T> resultType) throws IOException {
		return apply(defaultScope, jq, in, resultType);
	}

	public static <T> List<T> apply(final Scope scope, final JsonQuery jq, final Object in, final Class<T> resultType) throws IOException {
		return map(scope.getObjectMapper(), jq.apply(scope, (JsonNode) scope.getObjectMapper().valueToTree(in)), resultType);
	}

	@Deprecated
	public static <T> List<T> apply(final JsonQuery jq, final Object in, final TypeReference<T> resultType) throws IOException {
		return apply(defaultScope, jq, in, resultType);
	}

	public static <T> List<T> apply(final Scope scope, final JsonQuery jq, final Object in, final TypeReference<T> resultType) throws IOException {
		return map(scope.getObjectMapper(), jq.apply(scope, (JsonNode) scope.getObjectMapper().valueToTree(in)), resultType);
	}

	public static <T> List<T> map(final ObjectMapper mapper, final List<JsonNode> xs, final TypeReference<T> resultType) throws IOException {
		final List<T> result = new ArrayList<>();
		for (final JsonNode x : xs) {
			final T tmp = mapper.<T> readValue(mapper.treeAsTokens(x), resultType);
			result.add(tmp);
		}
		return result;
	}

	public static <T> List<T> map(final ObjectMapper mapper, final List<JsonNode> xs, final Class<T> resultType) throws IOException {
		final List<T> result = new ArrayList<>();
		for (final JsonNode x : xs)
			result.add(mapper.treeToValue(x, resultType));
		return result;
	}
}
