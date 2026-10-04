package net.thisptr.jackson.jq.v2.test.evaluator;

import net.thisptr.jackson.jq.v2.core.RuntimeOptions;

/**
 * Limits shared by expectation generation and the jackson-jq conformance tests.
 */
public final class EvaluationLimits {
	public static final RuntimeOptions OPTIONS = RuntimeOptions.newBuilder()
			.setMaxArrayLength(100_000)
			.setMaxObjectMemberCount(100_000)
			.setMaxStringLength(1_000_000)
			.setMaxBinaryLength(1_000_000)
			.setMaxUserDefinedFunctionCalls(100_000)
			.setMaxOutputsPerExpression(100_000)
			.build();

	private EvaluationLimits() {
	}
}
