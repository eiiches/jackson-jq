package net.thisptr.jackson.jq.v2.core.internal.ast.operator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryOperatorTableTest {
	@Test
	void everySupportedVersionDefinesEveryOperator() {
		for (BinaryOperatorTable table : new BinaryOperatorTable[] { Jq15BinaryOperatorTable.INSTANCE, Jq18BinaryOperatorTable.INSTANCE }) {
			for (BinaryOperator operator : BinaryOperator.values())
				assertThat(table.getOperatorInfo(operator)).as(operator.toString()).isNotNull();
		}
	}

	@Test
	void bindingPipePrecedenceChangesAtJq18() {
		assertThat(Jq15BinaryOperatorTable.INSTANCE.getOperatorInfo(BinaryOperator.BINDING_PIPE).getPrecedence()).isEqualTo(0);
		BinaryOperatorInfo jq18BindingPipe = Jq18BinaryOperatorTable.INSTANCE.getOperatorInfo(BinaryOperator.BINDING_PIPE);
		assertThat(jq18BindingPipe.getPrecedence()).isEqualTo(7);
		assertThat(jq18BindingPipe.getAssociativity()).isEqualTo(BinaryOperatorInfo.Associativity.RIGHT);
	}
}
