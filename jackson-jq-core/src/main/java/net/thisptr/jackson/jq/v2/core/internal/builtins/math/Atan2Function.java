package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "atan2", nargs = 2)
public class Atan2Function extends AbstractBinaryMathFunction {
	@Override
	protected double f(double a, double b) {
		return Math.atan2(a, b);
	}
}
