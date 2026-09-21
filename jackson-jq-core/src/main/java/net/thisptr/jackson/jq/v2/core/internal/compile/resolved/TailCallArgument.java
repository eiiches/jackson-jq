package net.thisptr.jackson.jq.v2.core.internal.compile.resolved;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.analysis.AnalyzedExpression;
import net.thisptr.jackson.jq.v2.core.internal.memory.StackFrame;
import net.thisptr.jackson.jq.v2.core.internal.utils.StackFrameValues;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

/**
 * One argument of a {@link ResolvedTailCall}, reduced to something the callee's frame can hold on its own.
 * <p>
 * An ordinary call hands a filter argument over as a {@code Function} that evaluates the argument expression
 * against the caller's frame, and binds a {@code $} parameter by running that expression once per value it
 * emits. Neither survives a tail call, which pops the caller's frame before the callee runs. So the compiler
 * only takes a call in tail position when every argument reduces to one of the two shapes below, and both are
 * resolved here, while the frame is still there.
 */
public interface TailCallArgument<JsonNode> {
	/**
	 * The argument emitted no value, so the call emits nothing -- exactly what {@code Compiler#bindValueParams}
	 * does when an argument runs dry before the body is reached.
	 */
	Object NO_VALUE = new Object();

	/**
	 * This argument cannot be resolved after all, and the call has to be made the ordinary way.
	 */
	Object FALL_BACK = new Object();

	/**
	 * Resolves this argument to what goes in the callee's parameter slot.
	 *
	 * @param frame the caller's frame, still current
	 * @param in the caller's input
	 * @param ipath the caller's input path
	 * @return the slot contents, or {@link #NO_VALUE} / {@link #FALL_BACK}
	 * @throws JsonQueryException if evaluating the argument does
	 */
	Object resolve(StackFrame frame, JsonNode in, Path<JsonNode> ipath) throws JsonQueryException;

	/**
	 * A {@code $name} parameter's argument, evaluated here rather than by the callee.
	 * <p>
	 * The compiler only builds one of these when the argument has {@link net.thisptr.jackson.jq.v2.spi.Cardinality#ONE},
	 * which is the same rule that makes the call a tail call at all: an argument emitting several values would
	 * need the body run several times, and that is not a jump. Zero and several are still handled, because
	 * cardinality is a promise a third-party {@code Function} can break.
	 */
	final class Value<JsonNode> implements TailCallArgument<JsonNode> {
		private final AnalyzedExpression<JsonNode> expression;

		public Value(AnalyzedExpression<JsonNode> expression) {
			this.expression = expression;
		}

		@Override
		public Object resolve(StackFrame frame, JsonNode in, Path<JsonNode> ipath) throws JsonQueryException {
			SingleValue<JsonNode> sink = new SingleValue<>();
			expression.apply(frame, in, ipath, sink);
			if (sink.count == 0)
				return NO_VALUE;
			if (sink.count > 1 || sink.value == null)
				return FALL_BACK;
			return sink.value;
		}

		// Stores what Compiler#bindValueParams would have stored: the value alone, path dropped.
		private static final class SingleValue<JsonNode> implements Output<JsonNode> {
			private int count;
			private @Nullable Object value;

			@Override
			public void emit(JsonNode out, Path<JsonNode> path) {
				count++;
				value = StackFrameValues.toSlot(out);
			}
		}
	}

	/**
	 * A filter parameter's argument that is a bare reference to a function already sitting in a slot, so the
	 * {@code Function} itself can be handed straight on. Anything more -- an argument that closes over the
	 * caller's locals -- is what stops a call being a tail call in the first place.
	 * <p>
	 * Handed on <em>as it is</em>, not wrapped. {@code Compiler#bindAndApply} would build a fresh
	 * {@code Function} evaluating the argument against the caller's frame, and passing a parameter along a
	 * recursion that way stacks one wrapper per iteration -- so invoking it at the end costs the Java frames
	 * the loop just saved. jq copies the closure reference for the same reason. The cost is that this call
	 * site's own output counter never ticks for this argument; what the function emits is still charged to the
	 * counter of the call site that first bound it, which is where the values actually come from.
	 */
	final class Filter<JsonNode> implements TailCallArgument<JsonNode> {
		private final int frameClosureSlot;
		private final int slot;

		public Filter(int frameClosureSlot, int slot) {
			this.frameClosureSlot = frameClosureSlot;
			this.slot = slot;
		}

		@Override
		public Object resolve(StackFrame frame, JsonNode in, Path<JsonNode> ipath) {
			Function factory = ResolvedTailCall.readFunction(frame, frameClosureSlot, slot);
			// Nothing in the slot is the "Function f is not defined" case; let the ordinary call raise it.
			return factory == null ? FALL_BACK : factory;
		}
	}
}
