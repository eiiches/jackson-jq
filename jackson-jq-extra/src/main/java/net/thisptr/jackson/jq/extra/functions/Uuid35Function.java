package net.thisptr.jackson.jq.extra.functions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.extra.internal.misc.UuidUtils;
import net.thisptr.jackson.jq.path.Path;

public class Uuid35Function implements Function {
	private final int uuidVersion;

	public Uuid35Function(int uuidVersion) {
		this.uuidVersion = uuidVersion;
	}

	@Override
	public void apply(Scope scope, List<Expression> args, JsonNode in, Path path, PathOutput output, Version version) throws JsonQueryException {
		Preconditions.checkInputType("uuid5", in, JsonNodeType.STRING, JsonNodeType.BINARY);

		args.get(0).apply(scope, in, (namespaceArg) -> {
			if (!namespaceArg.isTextual())
				throw new JsonQueryTypeException("namespace must be string, but got: %s", namespaceArg.getNodeType());
			UUID namespace;
			try {
				namespace = UUID.fromString(namespaceArg.asText());
			} catch (IllegalArgumentException e) {
				throw new JsonQueryException("namespace must be a valid UUID", e);
			}

			UUID uuid;
			if (in.isBinary()) {
				try {
					uuid = UuidUtils.uuid3or5(namespace, in.binaryValue(), this.uuidVersion);
				} catch (IOException e) {
					throw new JsonQueryException(e);
				}
			} else {
				uuid = UuidUtils.uuid3or5(namespace, in.asText().getBytes(StandardCharsets.UTF_8), this.uuidVersion);
			}

			output.emit(new TextNode(uuid.toString()), null);
		});
	}
}
