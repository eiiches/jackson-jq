package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.spi.Expression;

/**
 * Marker for a node produced directly by the parser, before compilation.
 * <p>
 * Unlike {@link Expression}, an {@code AstNode} may still reference
 * unresolved names (functions, variables, modules) and is never directly executable.
 */
public interface AstNode {
}
