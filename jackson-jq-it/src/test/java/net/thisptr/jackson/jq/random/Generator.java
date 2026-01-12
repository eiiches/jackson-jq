package net.thisptr.jackson.jq.random;

import java.util.List;

import net.thisptr.jackson.jq.Expression;

public interface Generator {
	int args();

	Expression generate(List<Expression> expressions);
}