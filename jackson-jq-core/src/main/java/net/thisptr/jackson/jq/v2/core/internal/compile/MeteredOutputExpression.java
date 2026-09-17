package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.Memory;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
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
 * Only an expression that can emit more than one value per input is wrapped (see
 * {@link Compiler#compile(net.thisptr.jackson.jq.v2.core.Environment, CompileContext, ModuleScope, net.thisptr.jackson.jq.v2.core.internal.ast.AstNode)}),
 * and the wrapper allocates its counting sink only for an invocation that actually has a budget: with none,
 * all this costs is the delegated call.
 * <p>
 * Every question the compiler asks of an expression has to reach the node underneath, or wrapping would
 * change how the query compiles. {@link FreeVariables} matters most: {@code FreeVariables#dependsOnVariables}
 * reads an expression that does not implement it as depending on variables, which would silently disable
 * constant folding. The same goes for {@link ConstantExpression}, which a {@code Function} is invited to
 * specialize on at bind time -- joni precompiles a constant regex that way -- so {@link #of} keeps a wrapped
 * constant recognisable as one.
 *
 * @param <JsonNode> the JSON node type
 */
class MeteredOutputExpression<JsonNode> implements Expression<StackFrame, JsonNode>, FreeVariables {
	protected final Expression<StackFrame, JsonNode> inner;
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
	static <JsonNode> Expression<StackFrame, JsonNode> of(Expression<StackFrame, JsonNode> inner, int index) {
		return inner instanceof ConstantExpression ? new MeteredConstantOutputExpression<>(inner, index) : new MeteredOutputExpression<>(inner, index);
	}

	MeteredOutputExpression(Expression<StackFrame, JsonNode> inner, int index) {
		this.inner = inner;
		this.index = index;
	}

	@Override
	public Cardinality getCardinality() {
		return inner.getCardinality();
	}

	@Override
	public boolean dependsOnInput() {
		return inner.dependsOnInput();
	}

	@Override
	public boolean dependsOnExternalState() {
		return inner.dependsOnExternalState();
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return FreeVariables.slotsOf(inner);
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return FreeVariables.opaqueIn(inner);
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
