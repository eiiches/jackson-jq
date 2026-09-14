package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Objects;
import java.util.function.Consumer;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

/**
 * The compiled query handed to callers. Every instance is immutable: {@link #withRuntimeOptions} and
 * {@link #withRuntimeBindings} return a new one sharing the same {@link RootExpression}, so a single compiled
 * query can be configured differently on each thread without any of them interfering.
 */
final class CompiledJsonQuery<JsonNode> implements JsonQuery<JsonNode> {
	private final RootExpression<JsonNode> rootExpr;
	private final RuntimeLimits runtimeLimits;
	private final RuntimeBindings<JsonNode> bindings;
	/*
	 * Whether `bindings` has already been through RootExpression#validateBindings. Only a query produced by
	 * withRuntimeBindings() has: the query compile() returns must stay unvalidated, or a query referencing a
	 * variable declared without a value would fail at compile time instead of letting the caller supply one
	 * afterwards.
	 */
	private final boolean bindingsValidated;

	CompiledJsonQuery(RootExpression<JsonNode> rootExpr, RuntimeLimits runtimeLimits, RuntimeBindings<JsonNode> bindings, boolean bindingsValidated) {
		this.rootExpr = rootExpr;
		this.runtimeLimits = runtimeLimits;
		this.bindings = bindings;
		this.bindingsValidated = bindingsValidated;
	}

	@Override
	public JsonQuery<JsonNode> withRuntimeOptions(RuntimeOptions options) {
		Objects.requireNonNull(options, "options");
		RuntimeLimits limits = new RuntimeLimitsImpl(options.getMaxArrayLength(), options.getMaxObjectMemberCount(), options.getMaxStringLength());
		return new CompiledJsonQuery<>(rootExpr, limits, bindings, bindingsValidated);
	}

	@Override
	public JsonQuery<JsonNode> withRuntimeBindings(RuntimeBindings<JsonNode> bindings) throws JsonQueryException {
		Objects.requireNonNull(bindings, "bindings");
		rootExpr.validateBindings(bindings);
		return new CompiledJsonQuery<>(rootExpr, runtimeLimits, bindings, true);
	}

	@Override
	public void apply(JsonNode in, Consumer<? super JsonNode> output) throws JsonQueryException {
		try {
			if (!bindingsValidated)
				rootExpr.validateBindings(bindings);
			rootExpr.apply(in, runtimeLimits, bindings, output);
		} catch (JsonQueryException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new JsonQueryException("Stack overflow during evaluation", e);
		} catch (RuntimeException e) {
			throw new JsonQueryException("Unexpected exception during evaluation", e);
		}
	}
}
