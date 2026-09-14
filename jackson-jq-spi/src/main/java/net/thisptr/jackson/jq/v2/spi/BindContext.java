package net.thisptr.jackson.jq.v2.spi;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * The compilation state one {@link Function#bind} call is given, alongside the arguments it
 * is binding.
 * <p>
 * It carries what is fixed for the whole compilation rather than for this one call -- the
 * {@link JsonProvider} the query is compiled against and the jq version it targets -- and it is the
 * extension point of that method: anything a {@code Function} comes to need at bind time is added
 * here, so the signature of {@link Function#bind} does not have to change again.
 * <p>
 * It pairs with {@link RuntimeContext} by phase: a {@code BindContext} is handed to a {@code Function}
 * when its call is bound, a {@code RuntimeContext} to the resulting {@link Expression} every time that
 * expression is evaluated.
 * <p>
 * Instances are immutable and safe to use from several threads at once. The engine supplies one.
 *
 * @param <JsonNode> the JSON node type
 */
public interface BindContext<JsonNode> {

	/**
	 * Returns the JSON provider the query is being compiled against.
	 * <p>
	 * Read it into a local variable at the top of {@link Function#bind} rather than calling
	 * this from inside the returned {@link Expression}: the expression is evaluated once per input, and
	 * capturing the provider itself keeps that path free of an extra indirection.
	 *
	 * @return the JSON provider, never {@code null}
	 */
	JsonProvider<JsonNode> getJsonProvider();

	/**
	 * Returns the jq compatibility version the query is being compiled for.
	 * <p>
	 * Read it into a local variable on the same terms as {@link #getJsonProvider()}.
	 *
	 * @return the jq version, never {@code null}
	 */
	Version getJqVersion();
}
