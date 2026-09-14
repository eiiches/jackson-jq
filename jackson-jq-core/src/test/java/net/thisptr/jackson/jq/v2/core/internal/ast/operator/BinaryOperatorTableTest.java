package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BinaryOperatorTableTest {
	@Test
	void everySupportedVersionDefinesEveryOperator() {
		for (BinaryOperatorTable table : new BinaryOperatorTable[] { Jq15BinaryOperatorTable.INSTANCE, Jq18BinaryOperatorTable.INSTANCE }) {
			for (BinaryOperator operator : BinaryOperator.values())
				assertNotNull(table.getOperatorInfo(operator), operator.toString());
		}
	}

	@Test
	void bindingPipePrecedenceChangesAtJq18() {
		assertEquals(0, Jq15BinaryOperatorTable.INSTANCE.getOperatorInfo(BinaryOperator.BINDING_PIPE).getPrecedence());
		BinaryOperatorInfo jq18BindingPipe = Jq18BinaryOperatorTable.INSTANCE.getOperatorInfo(BinaryOperator.BINDING_PIPE);
		assertEquals(7, jq18BindingPipe.getPrecedence());
		assertEquals(BinaryOperatorInfo.Associativity.RIGHT, jq18BindingPipe.getAssociativity());
	}
}
