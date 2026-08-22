package net.thisptr.jackson.jq.v2.ext.uuid.functions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import net.thisptr.jackson.jq.v2.ext.uuid.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.ext.uuid.internal.misc.UuidUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

public class Uuid35Function implements Function {
	private final int uuidVersion;

	public Uuid35Function(int uuidVersion) {
		this.uuidVersion = uuidVersion;
	}

	@Override
	public <JsonNode> Expression<JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<JsonNode>> args, Version version) {
		Expression<JsonNode> namespaceExpr = args.get(0);
		return (frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "uuid5", in, JsonNodeType.STRING, JsonNodeType.BINARY);

			namespaceExpr.apply(frame, in, null, (namespaceArg, opath) -> {
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
		};
	}
}
