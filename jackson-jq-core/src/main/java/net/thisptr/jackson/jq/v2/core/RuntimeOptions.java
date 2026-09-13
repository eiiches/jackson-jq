package net.thisptr.jackson.jq.v2.core;

import java.util.Objects;

import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;

/**
 * Settings for a single call to {@link JsonQuery#apply(Object, RuntimeOptions, JsonQueryBindings, java.util.function.Consumer)}.
 * <p>
 * Options are read when {@code apply()} is called, so changing a {@code RuntimeOptions} afterwards
 * never affects an invocation already under way, and one instance can be reused for any number of
 * invocations -- including concurrent ones, as long as it is not mutated while they run.
 */
public final class RuntimeOptions {
	// Used by JsonQuery's no-options overloads. Package-private so nothing outside core can mutate the
	// instance every default apply() shares.
	static final RuntimeOptions DEFAULT = new RuntimeOptions();

	private RuntimeLimits runtimeLimits = RuntimeLimitsImpl.UNLIMITED;

	/**
	 * Creates options with every setting at its default. Evaluation is unbounded until limits are set.
	 */
	public RuntimeOptions() {
	}

	/**
	 * Sets the budgets evaluation runs under.
	 * <p>
	 * By default nothing is bounded, so a query may allocate until the JVM runs out of memory, which
	 * is how jq itself behaves.
	 *
	 * @param runtimeLimits the limits to enforce
	 * @return this, for chaining
	 */
	public RuntimeOptions setRuntimeLimits(RuntimeLimits runtimeLimits) {
		this.runtimeLimits = Objects.requireNonNull(runtimeLimits, "runtimeLimits");
		return this;
	}

	/**
	 * Sets the largest number of elements an array produced during evaluation may have, leaving the
	 * other limits as they are.
	 *
	 * @param maxArrayLength the maximum array length; {@link Integer#MAX_VALUE} for no limit
	 * @return this, for chaining
	 * @throws IllegalArgumentException if {@code maxArrayLength} is negative
	 */
	public RuntimeOptions setMaxArrayLength(int maxArrayLength) {
		return setRuntimeLimits(new RuntimeLimitsImpl(maxArrayLength, runtimeLimits.getMaxObjectMemberCount(), runtimeLimits.getMaxStringLength()));
	}

	/**
	 * Sets the largest number of members an object produced during evaluation may have, leaving the
	 * other limits as they are.
	 *
	 * @param maxObjectMemberCount the maximum object member count; {@link Integer#MAX_VALUE} for no limit
	 * @return this, for chaining
	 * @throws IllegalArgumentException if {@code maxObjectMemberCount} is negative
	 */
	public RuntimeOptions setMaxObjectMemberCount(int maxObjectMemberCount) {
		return setRuntimeLimits(new RuntimeLimitsImpl(runtimeLimits.getMaxArrayLength(), maxObjectMemberCount, runtimeLimits.getMaxStringLength()));
	}

	/**
	 * Sets the largest number of characters a string produced during evaluation may have, leaving the
	 * other limits as they are.
	 * <p>
	 * Length is counted in UTF-16 code units, which is not the same as jq's {@code length}; see
	 * {@link RuntimeLimits#getMaxStringLength()}.
	 *
	 * @param maxStringLength the maximum string length in UTF-16 code units; {@link Integer#MAX_VALUE} for no limit
	 * @return this, for chaining
	 * @throws IllegalArgumentException if {@code maxStringLength} is negative
	 */
	public RuntimeOptions setMaxStringLength(int maxStringLength) {
		return setRuntimeLimits(new RuntimeLimitsImpl(runtimeLimits.getMaxArrayLength(), runtimeLimits.getMaxObjectMemberCount(), maxStringLength));
	}

	/**
	 * Returns the budgets evaluation runs under.
	 *
	 * @return the limits, never {@code null}
	 */
	public RuntimeLimits getRuntimeLimits() {
		return runtimeLimits;
	}
}
