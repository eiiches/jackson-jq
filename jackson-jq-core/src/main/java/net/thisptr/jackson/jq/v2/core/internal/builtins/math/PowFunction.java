package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

@FunctionRegistration(name = "pow", nargs = 2)
public class PowFunction extends AbstractBinaryMathFunction {
	@Override
	protected double f(double a, double b) {
		return Math.pow(a, b);
	}
}
