package net.thisptr.jackson.jq.internal.functions;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

public abstract class MathFunction implements Function {
	@Override
	public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
		Preconditions.checkInputType("mathfunc", in, JsonNodeType.NUMBER);
		return Collections.singletonList((JsonNode) new DoubleNode(f(in.asDouble())));
	}

	protected abstract double f(final double f);

	@BuiltinFunction("atan/0")
	public static class AtanFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.atan(v);
		}
	}

	@BuiltinFunction("cos/0")
	public static class CosFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.cos(v);
		}
	}

	@BuiltinFunction("floor/0")
	public static class FloorFunction extends MathFunction {
		@Override
		protected double f(double f) {
			return Math.floor(f);
		}
	}

	@BuiltinFunction("sin/0")
	public static class SinFunction extends MathFunction {
		@Override
		protected double f(double v) {
			return Math.sin(v);
		}
	}

	@BuiltinFunction("sqrt/0")
	public static class SqrtFunction extends MathFunction {
		@Override
		protected double f(final double v) {
			return Math.sqrt(v);
		}
	}

	@BuiltinFunction("log2/0")
	public static class Log2Function extends MathFunction {
		@Override
		protected double f(final double v) {
			return Math.log10(v) / Math.log10(2);
		}
	}
}
