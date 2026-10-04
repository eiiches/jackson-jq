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
		assertThat(jq18BindingPipe.getPrecedence()).isEqualTo(8);
		assertThat(jq18BindingPipe.getAssociativity()).isEqualTo(BinaryOperatorInfo.Associativity.RIGHT);
	}

	/**
	 * The levels jq declares in parser.y, loosest first: {@code |}, {@code ,}, {@code //}, the
	 * assignment operators, {@code or}, {@code and}, the comparisons, {@code + -}, {@code * / %}.
	 * The numbers are arbitrary, so this asserts the order rather than the values.
	 */
	@Test
	void everySupportedVersionOrdersLevelsAsJqDoes() {
		for (BinaryOperatorTable table : new BinaryOperatorTable[] { Jq15BinaryOperatorTable.INSTANCE, Jq18BinaryOperatorTable.INSTANCE }) {
			BinaryOperator[][] levels = {
					{ BinaryOperator.PIPE },
					{ BinaryOperator.COMMA },
					{ BinaryOperator.DEFAULT },
					{ BinaryOperator.ASSIGN, BinaryOperator.UPDATE, BinaryOperator.DEFAULT_EQUAL, BinaryOperator.PLUS_EQUAL,
							BinaryOperator.MINUS_EQUAL, BinaryOperator.TIMES_EQUAL, BinaryOperator.DIVIDE_EQUAL, BinaryOperator.MODULO_EQUAL },
					{ BinaryOperator.OR },
					{ BinaryOperator.AND },
					{ BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL, BinaryOperator.LESS, BinaryOperator.LESS_EQUAL,
							BinaryOperator.GREATER, BinaryOperator.GREATER_EQUAL },
					{ BinaryOperator.PLUS, BinaryOperator.MINUS },
					{ BinaryOperator.TIMES, BinaryOperator.DIVIDE, BinaryOperator.MODULO },
			};
			for (BinaryOperator[] level : levels) {
				int expected = table.getOperatorInfo(level[0]).getPrecedence();
				for (BinaryOperator operator : level) {
					assertThat(table.getOperatorInfo(operator).getPrecedence())
							.as("%s binds as tightly as %s", operator, level[0])
							.isEqualTo(expected);
				}
			}
			for (int i = 1; i < levels.length; i++) {
				BinaryOperator looser = levels[i - 1][0];
				BinaryOperator tighter = levels[i][0];
				assertThat(table.getOperatorInfo(tighter).getPrecedence())
						.as("%s binds tighter than %s", tighter, looser)
						.isLessThan(table.getOperatorInfo(looser).getPrecedence());
			}
		}
	}
}
