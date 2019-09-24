package net.thisptr.jackson.jq.extra.functions;

import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.path.Path;

@AutoService(Function.class)
@BuiltinFunction({ "hostname/0", "hostname/1" })
public class HostnameFunction implements Function {
	private JsonNode hostname = NullNode.getInstance();
	private JsonNode fqdn = NullNode.getInstance();

	public HostnameFunction() {
		try {
			final InetAddress addr = InetAddress.getLocalHost();
			this.hostname = new TextNode(addr.getHostName());
			this.fqdn = new TextNode(addr.getCanonicalHostName());
		} catch (Exception e) {
			/* ignore */
		}
	}

	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (arg) -> {
				if (arg.isTextual() && "fqdn".equals(arg.asText())) {
					output.emit(fqdn, null);
				} else {
					output.emit(hostname, null);
				}
			});
		} else {
			output.emit(hostname, null);
		}
	}
}