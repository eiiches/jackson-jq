package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

public abstract class MathFunction implements Function {
	@Override
	public <JsonNode> void apply(Scope<JsonNode> scope, List<Expression> args, JsonNode in, @Nullable Path<JsonNode> ipath, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
		JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "mathfunc", in, JsonNodeType.NUMBER);
		output.emit(jsonProvider.createDouble(f(jsonProvider.asDouble(in))), null);
	}

	protected abstract double f(double f);

	@AutoService(Function.class)
	@FunctionRegistration("atan/0")
	public static class AtanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("tan/0")
	public static class TanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.tan(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("tanh/0")
	public static class TanhFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.tanh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("acos/0")
	public static class AcosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.acos(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("cos/0")
	public static class CosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("cosh/0")
	public static class CoshFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cosh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("floor/0")
	public static class FloorFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("ceil/0")
	public static class CeilFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.ceil(f);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(value = "round/0", version = "[1.6, )")
	public static class RoundFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return v >= 0 ? Math.round(v) : -Math.round(-v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("asin/0")
	public static class AsinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.asin(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("sin/0")
	public static class SinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("sinh/0")
	public static class SinhFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sinh(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("cbrt/0")
	public static class CbrtFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cbrt(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("sqrt/0")
	public static class SqrtFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sqrt(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("log2/0")
	public static class Log2Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("log/0")
	public static class LogFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("log10/0")
	public static class Log10Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log10(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(value = "log1p/0", version = "[1.6, )")
	public static class Log1pFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.log1p(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("exp/0")
	public static class ExpFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.exp(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(value = "expm1/0", version = "[1.6, )")
	public static class Expm1Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.expm1(v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration("exp2/0")
	public static class Exp2Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(2, v);
		}
	}

	@AutoService(Function.class)
	@FunctionRegistration(value = "exp10/0", version = "[1.6, )")
	public static class Exp10Function extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.pow(10, v);
		}
	}
}
