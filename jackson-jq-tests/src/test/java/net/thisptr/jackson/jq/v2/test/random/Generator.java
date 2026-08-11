package net.thisptr.jackson.jq.v2.test.random;

import java.util.List;

import net.thisptr.jackson.jq.v2.spi.Expression;

public interface Generator {
	int args();

	Expression generate(List<Expression> expressions);
}
