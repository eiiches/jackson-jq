package net.thisptr.jackson.jq.extra.functions;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		if (args.size() == 1) {
			final List<JsonNode> out = new ArrayList<>();
			for (final JsonNode arg : args.get(0).apply(in)) {
				if (arg.isTextual() && "fqdn".equals(arg.asText())) {
					out.add(fqdn);
				} else {
					out.add(hostname);
				}
			}
			return out;
		}
		return Collections.singletonList(hostname);
	}
}