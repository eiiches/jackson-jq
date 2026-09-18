package net.thisptr.jackson.jq.v2.core;

/**
 * Settings a query runs under, applied with {@link JsonQuery#withRuntimeOptions(RuntimeOptions)}.
 * <p>
 * Instances are immutable, so one can be reused for any number of queries and invocations, including
 * concurrent ones. Build one with {@link #newBuilder()}.
 *
 * <h2>What these budgets meter</h2>
 * <p>
 * Evaluation -- the work a query does when it is applied to an input. That is not quite all the work a
 * query does: an expression that depends on neither the input, nor external state, nor a variable has the
 * same result every time, so the compiler evaluates it once while compiling and keeps the values. Such an
 * expression is never evaluated again, and so never draws on the budgets set here. {@code [range(0; 101)]}
 * builds its array at compile time and {@link Builder#setMaxArrayLength(int)} does not see it, while
 * {@code [range(0; .)]} on input {@code 101} is bounded normally.
 * <p>
 * Options attach to an already-compiled query, which is strictly after that has happened, so these numbers
 * cannot cover it by themselves. What bounds compile-time evaluation is {@link ConstantFoldingOptions},
 * set through {@link OptimizationOptions}: bounded by default and deliberately small, since an expression
 * that would exceed it is not folded at all, which puts it back on the evaluation path where these limits
 * apply to it in full. So the work a constant expression can do unmetered is bounded, just not by this
 * class, and it is done once per {@code compile()} rather than once per input.
 * <p>
 * Two ways to bring it under a caller's control. Passing the same {@code RuntimeOptions} to
 * {@link ConstantFoldingOptions.Builder#setRuntimeOptions(RuntimeOptions)} and to
 * {@link JsonQuery#withRuntimeOptions(RuntimeOptions)} folds under exactly the limits the query will run
 * under; {@link ConstantFoldingOptions.Builder#setEnabled(boolean) setEnabled(false)} folds nothing, so
 * every expression is evaluated and everything here meters it.
 */
public final class RuntimeOptions {
	private static final RuntimeOptions DEFAULT = new RuntimeOptions(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

	private final int maxArrayLength;
	private final int maxObjectMemberCount;
	private final int maxStringLength;
	private final long maxUserDefinedFunctionCalls;
	private final long maxOutputsPerExpression;

	private RuntimeOptions(int maxArrayLength, int maxObjectMemberCount, int maxStringLength, long maxUserDefinedFunctionCalls, long maxOutputsPerExpression) {
		this.maxArrayLength = maxArrayLength;
		this.maxObjectMemberCount = maxObjectMemberCount;
		this.maxStringLength = maxStringLength;
		this.maxUserDefinedFunctionCalls = maxUserDefinedFunctionCalls;
		this.maxOutputsPerExpression = maxOutputsPerExpression;
	}

	/**
	 * Creates a builder with every setting at its default. Evaluation is unbounded until limits are set.
	 *
	 * @return a new builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Returns the largest number of elements an array produced during evaluation may have.
	 *
	 * @return the maximum array length, or {@link Integer#MAX_VALUE} for no limit
	 */
	public int getMaxArrayLength() {
		return maxArrayLength;
	}

	/**
	 * Returns the largest number of members an object produced during evaluation may have.
	 *
	 * @return the maximum object member count, or {@link Integer#MAX_VALUE} for no limit
	 */
	public int getMaxObjectMemberCount() {
		return maxObjectMemberCount;
	}

	/**
	 * Returns the largest number of characters a string produced during evaluation may have.
	 * <p>
	 * Length is counted in UTF-16 code units, which is not the same as jq's {@code length}: that
	 * counts Unicode codepoints, so a string of astral characters such as {@code "😀"} has a
	 * {@code length} of 1 but a size of 2 here, and the limit can be up to twice as strict as
	 * {@code length} suggests.
	 *
	 * @return the maximum string length in UTF-16 code units, or {@link Integer#MAX_VALUE} for no limit
	 */
	public int getMaxStringLength() {
		return maxStringLength;
	}

	/**
	 * Returns the largest number of user-defined function calls one evaluation may make.
	 * <p>
	 * Only {@code def}s written in the query text handed to {@code Environment.compile()} are counted.
	 * Builtins -- whether implemented in Java or in jq itself -- and functions reached through an imported
	 * module never draw on the budget, so the number set here means the same thing regardless of how the
	 * engine happens to implement any particular builtin.
	 * <p>
	 * One call is one execution of the body. A {@code $}-parameter binds each value its argument produces
	 * in turn, so {@code def f($a): .; f(1, 2)} runs the body twice and costs two.
	 *
	 * @return the maximum number of user-defined function calls, or {@link Long#MAX_VALUE} for no limit
	 */
	public long getMaxUserDefinedFunctionCalls() {
		return maxUserDefinedFunctionCalls;
	}

	/**
	 * Returns the largest number of values a single expression may emit during one evaluation.
	 * <p>
	 * The budget is per expression, not per query: every expression written in the query text handed to
	 * {@code Environment.compile()} gets its own tally, and the evaluation fails as soon as any one of
	 * them exceeds this number. A tally covers the whole invocation rather than one input value, so an
	 * expression that is re-evaluated -- the right-hand side of a pipe, say -- accumulates across every
	 * re-evaluation: at a budget of 1000, {@code range(600)} passes but {@code range(600) | range(600)}
	 * fails, because the right-hand {@code range} runs 600 times and emits 360000 values in all.
	 * <p>
	 * Values emitted by builtins -- whether implemented in Java or in jq itself -- and by functions reached
	 * through an imported module are charged to the query-text expression that calls them, never to the
	 * library's own internals, so the number set here means the same thing regardless of how the engine
	 * happens to implement any particular builtin.
	 * <p>
	 * Expressions that cannot emit more than one value per input, such as {@code .foo} or a literal, are
	 * not tallied. They can only be evaluated as often as some enclosing generator emits, and that
	 * generator exceeds the budget no later than they would.
	 *
	 * @return the maximum number of values one expression may emit, or {@link Long#MAX_VALUE} for no limit
	 */
	public long getMaxOutputsPerExpression() {
		return maxOutputsPerExpression;
	}

	/**
	 * Builds a {@link RuntimeOptions}.
	 */
	public static final class Builder {
		private int maxArrayLength = Integer.MAX_VALUE;
		private int maxObjectMemberCount = Integer.MAX_VALUE;
		private int maxStringLength = Integer.MAX_VALUE;
		private long maxUserDefinedFunctionCalls = Long.MAX_VALUE;
		private long maxOutputsPerExpression = Long.MAX_VALUE;

		private Builder() {
		}

		/**
		 * Sets the largest number of elements an array produced during evaluation may have.
		 * <p>
		 * By default nothing is bounded, so a query may allocate until the JVM runs out of memory, which
		 * is how jq itself behaves.
		 *
		 * @param maxArrayLength the maximum array length; {@link Integer#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxArrayLength} is negative
		 */
		public Builder setMaxArrayLength(int maxArrayLength) {
			if (maxArrayLength < 0)
				throw new IllegalArgumentException("maxArrayLength must not be negative");
			this.maxArrayLength = maxArrayLength;
			return this;
		}

		/**
		 * Sets the largest number of members an object produced during evaluation may have.
		 * <p>
		 * By default nothing is bounded, so a query may allocate until the JVM runs out of memory, which
		 * is how jq itself behaves.
		 *
		 * @param maxObjectMemberCount the maximum object member count; {@link Integer#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxObjectMemberCount} is negative
		 */
		public Builder setMaxObjectMemberCount(int maxObjectMemberCount) {
			if (maxObjectMemberCount < 0)
				throw new IllegalArgumentException("maxObjectMemberCount must not be negative");
			this.maxObjectMemberCount = maxObjectMemberCount;
			return this;
		}

		/**
		 * Sets the largest number of characters a string produced during evaluation may have.
		 * <p>
		 * By default nothing is bounded, so a query may allocate until the JVM runs out of memory, which
		 * is how jq itself behaves. Length is counted in UTF-16 code units; see
		 * {@link RuntimeOptions#getMaxStringLength()}.
		 *
		 * @param maxStringLength the maximum string length in UTF-16 code units; {@link Integer#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxStringLength} is negative
		 */
		public Builder setMaxStringLength(int maxStringLength) {
			if (maxStringLength < 0)
				throw new IllegalArgumentException("maxStringLength must not be negative");
			this.maxStringLength = maxStringLength;
			return this;
		}

		/**
		 * Sets the largest number of user-defined function calls one evaluation may make.
		 * <p>
		 * By default nothing is bounded, so a runaway query such as {@code def f: f; f} runs until it
		 * exhausts the Java stack, which is how jq itself behaves. Only {@code def}s written in the query
		 * text are counted; see {@link RuntimeOptions#getMaxUserDefinedFunctionCalls()} for exactly what
		 * draws on the budget.
		 *
		 * @param maxUserDefinedFunctionCalls the maximum number of calls; {@link Long#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxUserDefinedFunctionCalls} is negative
		 */
		public Builder setMaxUserDefinedFunctionCalls(long maxUserDefinedFunctionCalls) {
			if (maxUserDefinedFunctionCalls < 0)
				throw new IllegalArgumentException("maxUserDefinedFunctionCalls must not be negative");
			this.maxUserDefinedFunctionCalls = maxUserDefinedFunctionCalls;
			return this;
		}

		/**
		 * Sets the largest number of values a single expression may emit during one evaluation.
		 * <p>
		 * By default nothing is bounded, so a runaway query such as {@code range(1e9) | empty} or
		 * {@code reduce range(0; 10000000) as $x (0; . + 1)} runs to completion, which is how jq itself
		 * behaves. Only expressions written in the query text are tallied, and each keeps its own count for
		 * the whole invocation; see {@link RuntimeOptions#getMaxOutputsPerExpression()} for exactly what
		 * draws on the budget.
		 *
		 * @param maxOutputsPerExpression the maximum number of values one expression may emit; {@link Long#MAX_VALUE} for no limit
		 * @return this, for chaining
		 * @throws IllegalArgumentException if {@code maxOutputsPerExpression} is negative
		 */
		public Builder setMaxOutputsPerExpression(long maxOutputsPerExpression) {
			if (maxOutputsPerExpression < 0)
				throw new IllegalArgumentException("maxOutputsPerExpression must not be negative");
			this.maxOutputsPerExpression = maxOutputsPerExpression;
			return this;
		}

		/**
		 * Builds the options.
		 *
		 * @return the options, never {@code null}
		 */
		public RuntimeOptions build() {
			if (maxArrayLength == Integer.MAX_VALUE && maxObjectMemberCount == Integer.MAX_VALUE && maxStringLength == Integer.MAX_VALUE && maxUserDefinedFunctionCalls == Long.MAX_VALUE && maxOutputsPerExpression == Long.MAX_VALUE)
				return DEFAULT;
			return new RuntimeOptions(maxArrayLength, maxObjectMemberCount, maxStringLength, maxUserDefinedFunctionCalls, maxOutputsPerExpression);
		}
	}
}
