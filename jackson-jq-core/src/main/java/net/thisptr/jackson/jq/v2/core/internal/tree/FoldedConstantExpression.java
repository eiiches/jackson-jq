package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.thisptr.jackson.jq.v2.core.internal.compile.freevars.FreeVariables;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

/**
 * A constant expression the compiler has already evaluated, emitting the values it found instead of running
 * again.
 * <p>
 * The values are the folder's own copies and are handed out by reference on every evaluation, which the
 * {@code JsonProvider} contract permits: its node type is immutable, and
 * {@link ConstantExpression#getConstantResults()} likewise requires callers to treat them as read-only.
 * Every value is emitted with {@link UntrackedPath}, the same as a literal.
 *
 * @param <JsonNode> the JSON node type
 */
public final class FoldedConstantExpression<JsonNode> implements ConstantExpression<StackFrame, JsonNode>, FreeVariables {
	private final Expression<StackFrame, JsonNode> delegate;
	private final List<JsonNode> values;

	/**
	 * @param delegate the expression that was folded, kept for path-tracked evaluation -- see {@link #apply}
	 * @param values the values {@code delegate} produced, in emission order; taken as-is, so the caller must
	 * pass a list nothing else will mutate
	 */
	public FoldedConstantExpression(Expression<StackFrame, JsonNode> delegate, List<JsonNode> values) {
		this.delegate = delegate;
		this.values = Collections.unmodifiableList(values);
	}

	@Override
	public List<JsonNode> getConstantResults() {
		return values;
	}

	@Override
	public Set<Integer> freeLocalSlots() {
		return Collections.emptySet();
	}

	@Override
	public boolean hasOpaqueVariableReference() {
		return false;
	}

	/**
	 * Emits the folded values -- unless a path is being tracked, in which case the folded expression runs
	 * after all.
	 * <p>
	 * An expression does not mean the same thing in both modes, so the values collected in one are not
	 * answers in the other: {@code [] | .c?} yields nothing when evaluated for its value, while
	 * {@code path([] | .c?)} is an error, because {@code ?} does not make an invalid path valid. The compiler
	 * folds by evaluating for values, so whatever it found is an answer only while nobody is asking where the
	 * values came from. A tracked input path is exactly that question being asked -- {@code path} hands its
	 * argument a {@code RootPath}, ordinary evaluation an {@link UntrackedPath} -- so this checks for one and
	 * steps out of the way, leaving path semantics, errors included, to the expression that owns them.
	 */
	@Override
	public void apply(StackFrame frame, JsonNode in, Path<JsonNode> ipath, Output<JsonNode> output) throws JsonQueryException {
		if (!(ipath instanceof UntrackedPath)) {
			delegate.apply(frame, in, ipath, output);
			return;
		}
		for (JsonNode value : values)
			output.emit(value, UntrackedPath.getInstance());
	}
}
