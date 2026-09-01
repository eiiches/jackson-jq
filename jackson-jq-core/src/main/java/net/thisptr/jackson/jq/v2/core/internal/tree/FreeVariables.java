package net.thisptr.jackson.jq.v2.core.internal.tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * Internal helper for precisely tracking free variable references. This is deliberately not part of
 * the public SPI: functions cannot directly access jq variables, and dependencies introduced through
 * function arguments are represented by the compiler-owned expression tree.
 */
public interface FreeVariables {
	/**
	 * Checks whether an expression contains a free or otherwise opaque variable reference.
	 *
	 * @param expr the expression to inspect, or {@code null}
	 * @return whether the expression depends on a variable
	 */
	static boolean dependsOnVariables(@Nullable Expression<?, ?> expr) {
		return !slotsOf(expr).isEmpty() || opaqueIn(expr);
	}

	/**
	 * Local slots (within the current function/root frame) read as free variables here.
	 */
	Set<Integer> freeLocalSlots();

	/**
	 * True if this subtree references a variable that can never be "closed" by a local binding --
	 * captured (crosses a def/closure boundary), global, or otherwise untracked.
	 */
	boolean hasOpaqueVariableReference();

	/**
	 * {@link #freeLocalSlots()} for an arbitrary, possibly-absent Expression, conservatively empty
	 * if {@code null} or untracked.
	 */
	static Set<Integer> slotsOf(@Nullable Expression<?, ?> expr) {
		return expr instanceof FreeVariables ? ((FreeVariables) expr).freeLocalSlots() : Collections.emptySet();
	}

	/**
	 * {@link #hasOpaqueVariableReference()} for an arbitrary, possibly-absent Expression,
	 * conservatively {@code false} if {@code null} (nothing to be opaque about), {@code true} if
	 * untracked.
	 */
	static boolean opaqueIn(@Nullable Expression<?, ?> expr) {
		return expr != null && (!(expr instanceof FreeVariables) || ((FreeVariables) expr).hasOpaqueVariableReference());
	}

	static Set<Integer> union(@Nullable Expression<?, ?>... exprs) {
		@Var Set<Integer> result = null;
		for (Expression<?, ?> e : exprs) {
			Set<Integer> s = slotsOf(e);
			if (s.isEmpty())
				continue;
			if (result == null)
				result = new HashSet<>();
			result.addAll(s);
		}
		return result == null ? Collections.emptySet() : result;
	}

	static Set<Integer> unionAll(List<? extends @Nullable Expression<?, ?>> exprs) {
		@Var Set<Integer> result = null;
		for (Expression<?, ?> e : exprs) {
			Set<Integer> s = slotsOf(e);
			if (s.isEmpty())
				continue;
			if (result == null)
				result = new HashSet<>();
			result.addAll(s);
		}
		return result == null ? Collections.emptySet() : result;
	}

	static boolean anyOpaque(@Nullable Expression<?, ?>... exprs) {
		for (Expression<?, ?> e : exprs)
			if (opaqueIn(e))
				return true;
		return false;
	}

	static boolean anyOpaqueIn(List<? extends @Nullable Expression<?, ?>> exprs) {
		for (Expression<?, ?> e : exprs)
			if (opaqueIn(e))
				return true;
		return false;
	}

	/** Subtracts the given slots from a set. Never mutates {@code slots}. */
	static Set<Integer> minus(Set<Integer> slots, List<Integer> toRemove) {
		if (slots.isEmpty() || toRemove.isEmpty())
			return slots;
		Set<Integer> result = new HashSet<>(slots);
		result.removeAll(toRemove);
		return result;
	}
}
