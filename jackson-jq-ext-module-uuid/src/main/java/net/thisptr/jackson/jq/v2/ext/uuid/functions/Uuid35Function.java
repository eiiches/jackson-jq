package net.thisptr.jackson.jq.v2.ext.uuid.functions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.ext.uuid.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.ext.uuid.internal.misc.UuidUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.LegacyFunction;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class Uuid35Function implements LegacyFunction {
	private final int uuidVersion;

	public Uuid35Function(int uuidVersion) {
		this.uuidVersion = uuidVersion;
	}

	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "uuid5", in, JsonNodeType.STRING, JsonNodeType.BINARY);

		args.get(0).apply(scope, in, (namespaceArg) -> {
			if (jsonProvider.getNodeType(namespaceArg) != JsonNodeType.STRING)
				throw new JsonQueryException(String.format("namespace must be string, but got: %s", jsonProvider.getNodeType(namespaceArg)));
			UUID namespace;
			try {
				namespace = UUID.fromString(jsonProvider.asText(namespaceArg));
			} catch (IllegalArgumentException e) {
				throw new JsonQueryException("namespace must be a valid UUID", e);
			}

			UUID uuid;
			if (jsonProvider.getNodeType(in) == JsonNodeType.BINARY) {
				uuid = UuidUtils.uuid3or5(namespace, jsonProvider.asByteArray(in), this.uuidVersion);
			} else {
				uuid = UuidUtils.uuid3or5(namespace, jsonProvider.asText(in).getBytes(StandardCharsets.UTF_8), this.uuidVersion);
			}

			output.emit(jsonProvider.createString(uuid.toString()), null);
		});
	}
}
