package net.thisptr.jackson.jq.v2.ext.os;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BooleanType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.type.UndefinedType;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;
import net.thisptr.jackson.jq.v2.spi.version.Version;

final class HostnameFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final Type OPTIONS = ObjectType.of("fqdn", UnionType.of(BooleanType.getInstance(), UndefinedType.getInstance()));
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(TypeScheme.of(FunctionType.of(AnyType.getInstance(), StringType.getInstance()))),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()),
					FunctionType.of(INPUT, StringType.getInstance(), FilterType.of(INPUT, OPTIONS)))));

	@FunctionalInterface
	interface HostnameLookup {
		HostNames lookup(boolean fqdn) throws UnknownHostException;
	}

	record HostNames(String hostname, String canonicalName, String address) {
	}

	private final HostnameLookup lookup;

	HostnameFunction() {
		this(HostnameFunction::localHostNames);
	}

	HostnameFunction(HostnameLookup lookup) {
		this.lookup = lookup;
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		return totalArguments >= 0 && totalArguments < TYPE_SCHEMES.size()
				? TYPE_SCHEMES.get(totalArguments) : List.of();
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		if (arguments.isEmpty())
			return new ExpressionProperties(Cardinality.ONE, false, true);
		ExpressionProperties options = arguments.get(0);
		return new ExpressionProperties(options.cardinality(), options.dependsOnInput(), true);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(
			BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> provider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> options = arguments.isEmpty() ? null : arguments.get(0);
		return new Expression<>() {
			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				if (options == null) {
					emit(provider, context, output, false);
					return;
				}
				options.apply(context, input, inputPath, (node, path) ->
						emit(provider, context, output, parseOptions(provider, node)));
			}
		};
	}

	private static <JsonNode> boolean parseOptions(JsonProvider<JsonNode> provider, JsonNode node) {
		JsonNodeType type = provider.getNodeType(node);
		if (type != JsonNodeType.OBJECT)
			throw new JsonQueryException("os::hostname options must be an object, but got " + type);
		Iterator<String> names = provider.getObjectMemberNames(node);
		while (names.hasNext()) {
			String name = names.next();
			if (!name.equals("fqdn"))
				throw new JsonQueryException("os::hostname options contains unknown member: " + name);
		}
		if (!provider.hasObjectMember(node, "fqdn"))
			return false;
		JsonNode value = provider.getObjectMemberOrThrow(node, "fqdn");
		if (!provider.isBoolean(value))
			throw new JsonQueryException("os::hostname fqdn must be a boolean");
		return provider.getBoolean(value);
	}

	private <Context extends RuntimeContext, JsonNode> void emit(JsonProvider<JsonNode> provider,
			Context context, Output<JsonNode> output, boolean fqdn) throws JsonQueryException {
		HostNames names;
		try {
			names = lookup.lookup(fqdn);
		} catch (UnknownHostException | SecurityException e) {
			throw new JsonQueryException("os::hostname failed: " + e.getMessage(), e);
		}
		String value = fqdn ? names.canonicalName() : names.hostname();
		if (fqdn && !isQualifiedName(value, names.address()))
			throw new JsonQueryException("os::hostname could not resolve a fully qualified domain name");
		int maximum = context.getRuntimeLimits().getMaxStringLength();
		if (value.length() > maximum)
			throw new RuntimeLimitExceededException("String of " + value.length()
					+ " characters exceeds the maximum string length of " + maximum);
		output.emit(provider.createString(value), UntrackedPath.getInstance());
	}

	private static boolean isQualifiedName(String name, String address) {
		int dot = name.indexOf('.');
		return dot > 0 && dot < name.length() - 1
				&& !name.equals(address)
				&& !name.contains(":")
				&& !name.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
	}

	private static HostNames localHostNames(boolean fqdn) throws UnknownHostException {
		InetAddress localHost = InetAddress.getLocalHost();
		String hostname = localHost.getHostName();
		return new HostNames(hostname, fqdn ? localHost.getCanonicalHostName() : hostname, localHost.getHostAddress());
	}
}
