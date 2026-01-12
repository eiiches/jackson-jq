package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public abstract class MathFunction<JsonNode> implements Function<JsonNode> {
	@Override
	public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
		final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
		Preconditions.checkInputType(jsonProvider, "mathfunc", in, JsonNodeType.NUMBER);
		output.emit(jsonProvider.createDouble(f(jsonProvider.asDouble(in))), null);
	}

	protected abstract double f(final double f);

	@AutoService(Function.class)
	@BuiltinFunction("atan/0")
	public static class AtanFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("tan/0")
	public static class TanFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.tan(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("tanh/0")
	public static class TanhFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.tanh(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("acos/0")
	public static class AcosFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.acos(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("cos/0")
	public static class CosFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("cosh/0")
	public static class CoshFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.cosh(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("floor/0")
	public static class FloorFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("ceil/0")
	public static class CeilFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double f) {
			return Math.ceil(f);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction(value = "round/0", version = "[1.6, )")
	public static class RoundFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return v >= 0 ? Math.round(v) : -Math.round(-v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("asin/0")
	public static class AsinFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.asin(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("sin/0")
	public static class SinFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("sinh/0")
	public static class SinhFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(double v) {
			return Math.sinh(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("cbrt/0")
	public static class CbrtFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.cbrt(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("sqrt/0")
	public static class SqrtFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.sqrt(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("log2/0")
	public static class Log2Function<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("log/0")
	public static class LogFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.log(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("log10/0")
	public static class Log10Function<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.log10(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction(value = "log1p/0", version = "[1.6, )")
	public static class Log1pFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.log1p(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("exp/0")
	public static class ExpFunction<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.exp(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction(value = "expm1/0", version = "[1.6, )")
	public static class Expm1Function<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.expm1(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("exp2/0")
	public static class Exp2Function<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.pow(2, v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction(value = "exp10/0", version = "[1.6, )")
	public static class Exp10Function<JsonNode> extends MathFunction<JsonNode> {
		@Override
		protected double f(final double v) {
			return Math.pow(10, v);
		}
	}
}
