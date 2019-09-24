package net.thisptr.jackson.jq.internal.functions;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

public abstract class MathFunction implements Function {
	@Override
	public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
		Preconditions.checkInputType("mathfunc", in, JsonNodeType.NUMBER);
		output.emit(new DoubleNode(f(in.asDouble())), null);
	}

	protected abstract double f(final double f);

	@AutoService(Function.class)
	@BuiltinFunction("atan/0")
	public static class AtanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("cos/0")
	public static class CosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("floor/0")
	public static class FloorFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("sin/0")
	public static class SinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("sqrt/0")
	public static class SqrtFunction extends MathFunction {
		@Override
		protected double f(final double v) {
			return Math.sqrt(v);
		}
	}

	@AutoService(Function.class)
	@BuiltinFunction("log2/0")
	public static class Log2Function extends MathFunction {
		@Override
		protected double f(final double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}
}
