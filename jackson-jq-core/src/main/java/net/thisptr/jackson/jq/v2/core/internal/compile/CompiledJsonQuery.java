package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeBindings;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;

/**
 * The compiled query handed to callers. Every instance is immutable: {@link #withRuntimeOptions} and
 * {@link #withRuntimeBindings} return a new one sharing the same {@link RootExpression}, so a single compiled
 * query can be configured differently on each thread without any of them interfering.
 */
final class CompiledJsonQuery<JsonNode> implements JsonQuery<JsonNode> {
	private final RootExpression<JsonNode> rootExpr;
	private final RuntimeLimitsImpl runtimeLimits;
	private final FilterType type;
	/*
	 * A null value means that this query references a declared global but withRuntimeBindings() has not supplied
	 * it. The query compile() returns must retain that state so the caller can bind it before apply(); apply()
	 * reports the missing binding if the caller does not.
	 */
	private final Object @Nullable [] globals;

	CompiledJsonQuery(RootExpression<JsonNode> rootExpr, RuntimeLimitsImpl runtimeLimits, FilterType type) {
		this(rootExpr, runtimeLimits, type, rootExpr.prepareEmptyBindings());
	}

	private CompiledJsonQuery(RootExpression<JsonNode> rootExpr, RuntimeLimitsImpl runtimeLimits, FilterType type, Object @Nullable [] globals) {
		this.rootExpr = rootExpr;
		this.runtimeLimits = runtimeLimits;
		this.type = type;
		this.globals = globals;
	}

	@Override
	public FilterType getType() {
		return type;
	}

	@Override
	public Cardinality getCardinality() {
		return rootExpr.getCardinality();
	}

	@Override
	public JsonQuery<JsonNode> withRuntimeOptions(RuntimeOptions options) {
		Objects.requireNonNull(options, "options");
		RuntimeLimitsImpl limits = new RuntimeLimitsImpl(options.getMaxArrayLength(), options.getMaxObjectMemberCount(), options.getMaxStringLength(), options.getMaxBinaryLength(), options.getMaxUserDefinedFunctionCalls(), options.getMaxOutputsPerExpression());
		return new CompiledJsonQuery<>(rootExpr, limits, type, globals);
	}

	@Override
	public JsonQuery<JsonNode> withRuntimeBindings(RuntimeBindings<JsonNode> bindings) throws JsonQueryException {
		Objects.requireNonNull(bindings, "bindings");
		return new CompiledJsonQuery<>(rootExpr, runtimeLimits, type, rootExpr.prepareBindings(bindings));
	}

	@Override
	public void apply(JsonNode in, Consumer<? super JsonNode> output) throws JsonQueryException {
		try {
			Object[] effectiveGlobals = globals != null ? globals : rootExpr.prepareBindings(null);
			rootExpr.apply(in, runtimeLimits, effectiveGlobals, output);
		} catch (JsonQueryException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new JsonQueryException("Stack overflow during evaluation", e);
		} catch (RuntimeException e) {
			throw new JsonQueryException("Unexpected exception during evaluation", e);
		}
	}
}
