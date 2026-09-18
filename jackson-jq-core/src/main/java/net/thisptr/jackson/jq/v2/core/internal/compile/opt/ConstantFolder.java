package net.thisptr.jackson.jq.v2.core.internal.compile.opt;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.ConstantFoldingOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.misc.RuntimeLimitsImpl;
import net.thisptr.jackson.jq.v2.core.internal.tree.FoldedConstantExpression;
import net.thisptr.jackson.jq.v2.core.internal.tree.FoldedErrorExpression;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * Evaluates a constant expression while the query is being compiled and replaces it with its outcome.
 * <p>
 * {@link FoldPlanner} offers maximal constant subtrees first. A successful fold stops traversal into that
 * subtree; a refused fold leaves the expression intact and lets the planner consider its children.
 * <p>
 * An expression is constant when it depends on neither the input, nor external state, nor a variable (see
 * {@code docs/constant-expression.md}). That is necessary but not sufficient to fold: a constant expression
 * may also loop forever, hit a budget below, or fail outside jq's error model. Those outcomes abandon the
 * fold. An ordinary {@link JsonQueryException}, however, is part of jq evaluation: the folder records it
 * after any values already emitted and replays the complete outcome when the query runs.
 * <p>
 * One instance holds one compilation's budget, derived once from the caller's
 * {@link ConstantFoldingOptions}, and how much of it has been spent so far. Exceeding any of it abandons the
 * fold and leaves the expression to run at evaluation time -- it is never a compilation failure. These are
 * therefore a statement about how much work is worth doing speculatively, not a safety boundary a query
 * could be rejected for crossing.
 * <p>
 * They are also what bounds the work a constant expression may do <em>without</em> being charged to the
 * caller's own {@link RuntimeOptions}, since those meter evaluation and a folded expression is not evaluated
 * any more. That is why the defaults are small, and why a caller who needs those budgets to cover everything
 * can turn folding off instead; see {@link ConstantFoldingOptions}.
 */
final class ConstantFolder {

	/**
	 * Stops a fold that has outgrown its budget.
	 * <p>
	 * Deliberately <em>not</em> a {@link JsonQueryException}: a {@code try} inside the expression being
	 * folded catches those, and would turn "this fold is too big" into a caught jq error and a wrongly
	 * folded value. Nothing in the engine catches a bare {@link RuntimeException}, so this reaches
	 * {@link #fold} instead. {@code Collector#breached} is the backstop for the one thing that could still
	 * swallow it -- a {@code Function} catching {@code Throwable}.
	 */
	private static final class FoldAbandonedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		FoldAbandonedException() {
			// No message, no stack trace: it never reaches a user and is thrown on a hot compile path.
			super(null, null, /* enableSuppression */ false, /* writableStackTrace */ false);
		}
	}

	private final boolean enabled;

	/**
	 * The most values one folded expression may produce.
	 */
	private final int maxResults;

	/**
	 * The most values every fold in this compilation may produce between them.
	 * <p>
	 * Folding is attempted top-down. A refused maximal subtree is followed by attempts on its children, so
	 * the expensive part of a refusal can still be revisited at successive levels. A chain of k constant
	 * terms under d wrappers can therefore cost O(k*d), and both grow with the query text, so without this
	 * the work one compilation can do is quadratic in the length of the query. With it, compiling
	 * {@code [reduce range(0;300) as $x (0; .), ...x200]} under 200 pipe levels stays flat instead of
	 * re-running the chain once per level.
	 */
	private final long maxTotalResults;

	/**
	 * The limits a fold's {@code Memory} runs under.
	 */
	private final RuntimeLimitsImpl limits;

	/**
	 * How many values this compilation's folds have produced so far, successful or not.
	 */
	private long values;

	/**
	 * Derives the budget for one compilation. Done once, rather than per fold attempt, because converting
	 * the options allocates.
	 *
	 * @param options the caller's settings
	 */
	ConstantFolder(ConstantFoldingOptions options) {
		RuntimeOptions runtimeOptions = options.getRuntimeOptions();
		this.enabled = options.isEnabled();
		this.maxResults = options.getMaxResults();
		// Scaled from maxResults rather than fixed, so raising one raises the other and no hidden ceiling
		// silently dominates the number the caller set.
		this.maxTotalResults = 256L * options.getMaxResults();
		this.limits = new RuntimeLimitsImpl(runtimeOptions.getMaxArrayLength(), runtimeOptions.getMaxObjectMemberCount(),
				runtimeOptions.getMaxStringLength(), runtimeOptions.getMaxBinaryLength(), runtimeOptions.getMaxUserDefinedFunctionCalls(), runtimeOptions.getMaxOutputsPerExpression());
	}

	/**
	 * Whether anything may be folded at all.
	 */
	boolean isEnabled() {
		return enabled;
	}

	/**
	 * Folds {@code compiled} to its values and optional terminal jq error if it can be evaluated within
	 * what is left of this compilation's budget.
	 *
	 * @param env the environment the query is compiling against
	 * @param compiled the freshly lowered expression
	 * @param frameSize the frame size required by this expression's compile position
	 * @param globalCount the number of global slots assigned when this expression finished lowering
	 * @param outputCounterCount the number of output counters assigned when this expression finished lowering
	 * @param <N> the JSON node type
	 * @return an expression carrying the folded outcome, or {@code compiled} unchanged
	 */
	<N> Expression<StackFrame, N> fold(Environment<N> env, Expression<StackFrame, N> compiled, int frameSize, int globalCount, int outputCounterCount) {
		if (isExhausted())
			return compiled;

		JsonProvider<N> jsonProvider = env.getJsonProvider();
		// A real frame, sized to the slots assigned so far, rather than an empty one: a jq-library body that
		// JqFunctionCompiler inlined reads its parameters out of the *enclosing* frame, at slots taken from
		// the caller's high-water mark (see CompileContext#pushInlinedFunctionScope), so those slots have to
		// exist. Globals are all null, which is safe because every global access is conservatively opaque and
		// so never reaches here.
		Memory memory = new Memory(new Object[globalCount], limits, outputCounterCount);
		StackFrame frame = memory.pushFrame(frameSize);
		Collector<N> collector = new Collector<>(jsonProvider);
		@Var JsonQueryException terminalError = null;
		@Var boolean abandoned = false;
		try {
			compiled.apply(frame, jsonProvider.createNull(), UntrackedPath.getInstance(), collector);
		} catch (FoldAbandonedException e) {
			abandoned = true;
		} catch (RuntimeLimitExceededException e) {
			abandoned = true;
		} catch (JsonQueryException e) {
			terminalError = e;
		} catch (RuntimeException | StackOverflowError e) {
			// A Function that throws something other than JsonQueryException, or recursion deep enough to
			// exhaust the Java stack. Compilation must not fail for either: the query is allowed to be
			// broken, and it has to break where a caller can catch it.
			abandoned = true;
		} finally {
			memory.popFrame();
		}
		if (abandoned || collector.breached)
			return compiled;

		if (terminalError != null)
			return new FoldedErrorExpression<>(compiled, collector.values, terminalError);
		return new FoldedConstantExpression<>(compiled, collector.values);
	}

	/**
	 * Whether the compilation-wide budget is used up, in which case no further fold is attempted.
	 */
	private boolean isExhausted() {
		return values >= maxTotalResults;
	}

	/**
	 * Charges one value produced by a fold -- whether or not that fold goes on to succeed, since an
	 * abandoned fold costs the same work as a successful one.
	 *
	 * @return {@code false} if this value is past the compilation-wide budget, which the caller must then
	 * discard
	 */
	private boolean charge() {
		// `<=`, so the budget admits exactly maxTotalResults values rather than one fewer -- matching the
		// per-expression cap in the collector below and Memory's own counters, which both fail only once a
		// count has *exceeded* its maximum. The refused value is still counted, so `values` saturates at
		// maxTotalResults and isExhausted() then skips every later fold without evaluating it.
		return ++values <= maxTotalResults;
	}

	private final class Collector<N> implements Output<N> {
		private final JsonProvider<N> jsonProvider;
		final List<N> values = new ArrayList<>();

		// Set the moment a budget is exceeded, and checked again after apply() returns. FoldAbandonedException
		// alone would be enough if every Function let it through, but one catching Throwable would swallow it
		// and the fold would finish with a truncated value list. A flag cannot be swallowed.
		boolean breached;

		Collector(JsonProvider<N> jsonProvider) {
			this.jsonProvider = jsonProvider;
		}

		@Override
		public void emit(N value, Path<N> path) {
			if (breached || values.size() >= maxResults || !charge()) {
				breached = true;
				throw new FoldAbandonedException();
			}
			// The values outlive this evaluation, so take the folder's own copy rather than trusting a
			// Function not to hand out something it reuses. An immutable provider returns the same instance.
			values.add(jsonProvider.deepCopy(value));
		}
	}
}
