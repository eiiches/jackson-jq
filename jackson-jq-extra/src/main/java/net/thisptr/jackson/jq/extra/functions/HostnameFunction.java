package net.thisptr.jackson.jq.extra.functions;

import java.net.InetAddress;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.path.Path;

@SuppressWarnings("rawtypes")
@AutoService(Function.class)
@BuiltinFunction({ "hostname/0", "hostname/1" })
public class HostnameFunction<JsonNode> implements Function<JsonNode> {
	private String hostname = null;
	private String fqdn = null;

	public HostnameFunction() {
		try {
			final InetAddress addr = InetAddress.getLocalHost();
			this.hostname = addr.getHostName();
			this.fqdn = addr.getCanonicalHostName();
		} catch (Exception e) {
			/* ignore */
		}
	}

	private JsonNode getHostnameNode(final JsonProvider<JsonNode> jsonProvider) {
		return hostname != null ? jsonProvider.createString(hostname) : jsonProvider.createNull();
	}

	private JsonNode getFqdnNode(final JsonProvider<JsonNode> jsonProvider) {
		return fqdn != null ? jsonProvider.createString(fqdn) : jsonProvider.createNull();
	}

	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (arg) -> {
				if (jsonProvider.asText(arg) != null && "fqdn".equals(jsonProvider.asText(arg))) {
					output.emit(getFqdnNode(jsonProvider), null);
				} else {
					output.emit(getHostnameNode(jsonProvider), null);
				}
			});
		} else {
			output.emit(getHostnameNode(jsonProvider), null);
		}
	}
}
