package net.thisptr.jackson.jq.v2.core.internal.builtins.math;

import java.util.List;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public class MathFunctions {

	public abstract static class AbstractMathFunction implements Function {
		@Override
		public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
			return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
				Preconditions.checkInputType(jsonProvider, "mathfunc", in, JsonNodeType.NUMBER);
				output.emit(jsonProvider.createNumber(f(jsonProvider.getNumberAsDoubleRounded(in))), UntrackedPath.getInstance());
			});
		}

		protected abstract double f(double f);
	}

	@FunctionRegistration(name = "atan", nargs = 0)
	public static class AtanFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@FunctionRegistration(name = "tan", nargs = 0)
	public static class TanFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.tan(v);
		}
	}

	@FunctionRegistration(name = "tanh", nargs = 0)
	public static class TanhFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.tanh(v);
		}
	}

	@FunctionRegistration(name = "acos", nargs = 0)
	public static class AcosFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.acos(v);
		}
	}

	@FunctionRegistration(name = "cos", nargs = 0)
	public static class CosFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@FunctionRegistration(name = "cosh", nargs = 0)
	public static class CoshFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.cosh(v);
		}
	}

	@FunctionRegistration(name = "floor", nargs = 0)
	public static class FloorFunction extends AbstractMathFunction {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@FunctionRegistration(name = "ceil", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class CeilFunction extends AbstractMathFunction {
		@Override
		protected double f(double f) {
			return Math.ceil(f);
		}
	}

	@FunctionRegistration(name = "round", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class RoundFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return v >= 0 ? Math.round(v) : -Math.round(-v);
		}
	}

	@FunctionRegistration(name = "asin", nargs = 0)
	public static class AsinFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.asin(v);
		}
	}

	@FunctionRegistration(name = "sin", nargs = 0)
	public static class SinFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@FunctionRegistration(name = "sinh", nargs = 0)
	public static class SinhFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.sinh(v);
		}
	}

	@FunctionRegistration(name = "cbrt", nargs = 0)
	public static class CbrtFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.cbrt(v);
		}
	}

	@FunctionRegistration(name = "sqrt", nargs = 0)
	public static class SqrtFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.sqrt(v);
		}
	}

	@FunctionRegistration(name = "log2", nargs = 0)
	public static class Log2Function extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}

	@FunctionRegistration(name = "log", nargs = 0)
	public static class LogFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.log(v);
		}
	}

	@FunctionRegistration(name = "log10", nargs = 0)
	public static class Log10Function extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v);
		}
	}

	@FunctionRegistration(name = "log1p", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Log1pFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.log1p(v);
		}
	}

	@FunctionRegistration(name = "exp", nargs = 0)
	public static class ExpFunction extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.exp(v);
		}
	}

	@FunctionRegistration(name = "expm1", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Expm1Function extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.expm1(v);
		}
	}

	@FunctionRegistration(name = "exp2", nargs = 0)
	public static class Exp2Function extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(2, v);
		}
	}

	@FunctionRegistration(name = "exp10", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Exp10Function extends AbstractMathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(10, v);
		}
	}
}
