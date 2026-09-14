package net.thisptr.jackson.jq.v2.fuzz;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public interface Generator {
	int args();

	AstNode generate(List<AstNode> expressions);
}
