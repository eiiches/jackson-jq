package net.thisptr.jackson.jq.extra.functions;

import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.Output;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

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
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Output output) throws JsonQueryException {
		if (args.size() == 1) {
			args.get(0).apply(scope, in, (arg) -> {
				if (arg.isTextual() && "fqdn".equals(arg.asText())) {
					output.emit(fqdn);
				} else {
					output.emit(hostname);
				}
			});
		} else {
			output.emit(hostname);
		}
	}
}