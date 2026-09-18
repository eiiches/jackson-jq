package net.thisptr.jackson.jq.v2.core.internal.tree;

import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * Tallies everything one query-text expression emits against
 * {@code RuntimeOptions.Builder#setMaxOutputsPerExpression(long)}.
 * <p>
 * The compiler wraps a node in this exactly where the budget has to be observed -- around the expression
 * itself, rather than at the {@code output.emit} calls scattered across the engine -- so a value a builtin
 * or a third-party {@code Function} produces is charged to the query-text expression that called it, and
 * a node reached through a jq-library body or an imported module is never wrapped and so never charged.
 * <p>
 * Function arguments are the only position wrapped this way (see {@code Compiler}'s
 * {@code meterArguments}), and the wrapper allocates its counting sink only for an invocation that
 * actually has a budget: with none, all this costs is the delegated call.
 * <p>
 * Every question the compiler asks of an expression has to reach the node underneath, which is why this
 * extends {@link AbstractDelegatingExpression} -- see that class for what breaks otherwise.
 *
 * @param <JsonNode> the JSON node type
 */
public class MeteredOutputExpression<JsonNode> extends AbstractDelegatingExpression<JsonNode> {
	private final int index;

	/**
	 * Wraps {@code inner} so its output is tallied, keeping it a {@link ConstantExpression} if it already was
	 * one. A constant-folded argument that stopped answering {@code instanceof ConstantExpression} would cost
	 * every {@code Function} that specializes on constants its specialization.
	 *
	 * @param inner the expression to meter
	 * @param index its output counter index
	 * @param <JsonNode> the JSON node type
	 * @return the metered expression
	 */
	public static <JsonNode> Expression<StackFrame, JsonNode> of(Expression<StackFrame, JsonNode> inner, int index) {
		return inner instanceof ConstantExpression ? new MeteredConstantOutputExpression<>(inner, index) : new MeteredOutputExpression<>(inner, index);
	}

	MeteredOutputExpression(Expression<StackFrame, JsonNode> inner, int index) {
		super(inner);
		this.index = index;
	}

	@Override
	protected Expression<StackFrame, JsonNode> recreate(Expression<StackFrame, JsonNode> rewrittenInner) {
		return of(rewrittenInner, index);
	}

	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> path, Output<JsonNode> output) throws JsonQueryException {
		Memory memory = frame.getEnclosingMemory();
		if (!memory.metersOutputs()) {
			inner.apply(frame, in, path, output);
			return;
		}
		inner.apply(frame, in, path, (value, valuePath) -> {
			memory.countOutput(index);
			output.emit(value, valuePath);
		});
	}
}
