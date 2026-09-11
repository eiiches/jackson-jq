package net.thisptr.jackson.jq.v2.core.internal.ast;

/**
 * A node produced directly by the parser, before compilation.
 * <p>
 * Unlike {@link net.thisptr.jackson.jq.v2.spi.Expression}, an {@code AstNode} may still reference
 * unresolved names (functions, variables, modules) and is never directly executable.
 */
public interface AstNode {
	<R> R accept(AstVisitor<R> visitor);
}
