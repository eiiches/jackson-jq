package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.function.Supplier;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.compile.Closure;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.StackFrame;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public class ResolvedGlobalVariableAccess<JsonNode> implements Expression<JsonNode> {
	private final String name;
	private final int slot;
	private final boolean captured;
	private final int frameClosureSlot;
	private final Supplier<JsonNode> defaultSupplier;

	public ResolvedGlobalVariableAccess(String name, int slot, boolean captured, int frameClosureSlot, Supplier<JsonNode> defaultSupplier) {
		this.name = name;
		this.slot = slot;
		this.captured = captured;
		this.frameClosureSlot = frameClosureSlot;
		this.defaultSupplier = defaultSupplier;
	}

	@Override
	public void apply(@Nullable StackFrame frame, JsonNode in, @Nullable Path<JsonNode> path, PathOutput<JsonNode> output) throws JsonQueryException {
		@Var Supplier<JsonNode> valueSupplier = null;
		if (captured) {
			Closure closure = frame != null ? (Closure) frame.get(frameClosureSlot) : null;
			Object raw = closure != null ? closure.get(slot) : null;
			if (raw instanceof Supplier) {
				@SuppressWarnings("unchecked")
				Supplier<JsonNode> effectiveSupplier = (Supplier<JsonNode>) raw;
				valueSupplier = effectiveSupplier;
			}
		} else if (frame != null) {
			Object raw = frame.get(slot);
			if (raw instanceof Supplier) {
				@SuppressWarnings("unchecked")
				Supplier<JsonNode> effectiveSupplier = (Supplier<JsonNode>) raw;
				valueSupplier = effectiveSupplier;
			}
		}
		if (valueSupplier == null)
			valueSupplier = defaultSupplier;
		if (valueSupplier == null)
			throw new JsonQueryException(String.format("Variable $%s is not defined", name));
		JsonNode val = valueSupplier.get();
		if (val == null)
			throw new JsonQueryException(String.format("Variable $%s evaluated to null", name));
		output.emit(val, null);
	}

	@Override
	public String toString() {
		return "$" + name;
	}
}
