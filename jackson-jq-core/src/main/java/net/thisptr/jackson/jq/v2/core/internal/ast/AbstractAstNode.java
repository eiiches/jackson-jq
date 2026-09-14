package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.Objects;

import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;

/**
 * Base for every {@link AstNode}, carrying the region of source text the node was parsed from.
 */
public abstract class AbstractAstNode implements AstNode {
	private final SourceLocation location;

	protected AbstractAstNode(SourceLocation location) {
		this.location = Objects.requireNonNull(location, "location");
	}

	@Override
	public final SourceLocation location() {
		return location;
	}
}
