package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.Set;

/**
 * Precomputed {@code dependsOn*()} facts about a local {@code def}'s body, recorded (see
 * {@link CompileContext#recordFunctionDependsOnInfo}) once its {@code ResolvedFunctionDefinition} finishes
 * compiling, and consulted by later call sites in the same/nested scope (see
 * {@link CompileContext#getFunctionLocation}) so they don't have to conservatively assume the worst.
 *
 * <p>{@code freeLocalSlots}/{@code hasOpaqueVariableReference} describe which variables invoking this
 * function depends on, numbered relative to the def's own <em>enclosing</em> frame (i.e. the same frame a
 * same-frame/local call site runs in) -- not the def's own body frame.
 */
public record FunctionDependsOnInfo(boolean dependsOnInput, boolean dependsOnExternalState, Set<Integer> freeLocalSlots,
									boolean hasOpaqueVariableReference) {

}
