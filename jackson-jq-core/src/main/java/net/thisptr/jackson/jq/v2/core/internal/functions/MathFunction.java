package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionRangeSpec;
import net.thisptr.jackson.jq.v2.spi.annotations.VersionSpec;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public abstract class MathFunction implements Function {

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((scope, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, "mathfunc", in, JsonNodeType.NUMBER);
			output.emit(jsonProvider.createNumber(f(jsonProvider.asDoubleRounded(in))), UntrackedPath.getInstance());
		});
	}

	protected abstract double f(double f);

	@AutoService(Function.class)
	@FunctionRegistration(name = "atan", nargs = 0)
	public static class AtanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "tan", nargs = 0)
	public static class TanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.tan(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "tanh", nargs = 0)
	public static class TanhFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.tanh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "acos", nargs = 0)
	public static class AcosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.acos(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "cos", nargs = 0)
	public static class CosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "cosh", nargs = 0)
	public static class CoshFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cosh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "floor", nargs = 0)
	public static class FloorFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "ceil", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class CeilFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.ceil(f);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "round", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class RoundFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return v >= 0 ? Math.round(v) : -Math.round(-v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "asin", nargs = 0)
	public static class AsinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.asin(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "sin", nargs = 0)
	public static class SinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "sinh", nargs = 0)
	public static class SinhFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sinh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "cbrt", nargs = 0)
	public static class CbrtFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cbrt(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "sqrt", nargs = 0)
	public static class SqrtFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sqrt(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "log2", nargs = 0)
	public static class Log2Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "log", nargs = 0)
	public static class LogFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "log10", nargs = 0)
	public static class Log10Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "log1p", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Log1pFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log1p(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "exp", nargs = 0)
	public static class ExpFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.exp(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "expm1", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Expm1Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.expm1(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "exp2", nargs = 0)
	public static class Exp2Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(2, v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(name = "exp10", nargs = 0, version = @VersionRangeSpec(
			min = @VersionSpec(major = 1, minor = 6, patch = 0)
	))
	public static class Exp10Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(10, v);
		}
	}
}
