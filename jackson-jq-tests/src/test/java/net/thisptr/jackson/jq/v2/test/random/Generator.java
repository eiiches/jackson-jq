package net.thisptr.jackson.jq.v2.test.random;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public interface Generator {
	int args();

	AstNode generate(List<AstNode> expressions);
}
