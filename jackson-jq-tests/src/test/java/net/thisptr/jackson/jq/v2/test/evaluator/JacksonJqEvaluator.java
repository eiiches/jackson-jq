package net.thisptr.jackson.jq.v2.test.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.internal.javacc.ExpressionParser;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.test.DefaultRootScope;

public class JacksonJqEvaluator implements Evaluator {

	private Result doEvaluate(Expression<JsonNode> expr, JsonNode in, Version version) throws JsonQueryException {
		List<JsonNode> values = new ArrayList<>();
		Scope<JsonNode> scope = Scope.newChildScope(DefaultRootScope.getInstance(version));
		try {
			expr.apply(scope, in, null, (out, opath) -> {
				@Var JsonNode value = out;
				if (out.isNumber() && Double.isNaN(out.asDouble()))
					value = NullNode.getInstance();
				if (out.isNumber() && Double.isInfinite(out.asDouble()))
					value = DoubleNode.valueOf(out.asDouble() > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE);
				values.add(value);
			}, false);
			return new Result(values, null);
		} catch (Throwable th) {
			return new Result(values, th);
		}
	}

	@SuppressWarnings("deprecation")
	private static void terminateThread(Thread thread) {
		thread.stop();
	}

	@Override
	public Result evaluate(String exprText, JsonNode in, Version version, long timeout) throws Throwable {
		AtomicReference<Result> result = new AtomicReference<>();
		AtomicReference<Throwable> exception = new AtomicReference<>();
		Thread th = new Thread() {
			@Override
			public void run() {
				try {
					Expression<JsonNode> expr = ExpressionParser.compile(exprText, version);
					result.set(doEvaluate(expr, in, version));
				} catch (Throwable e) {
					exception.set(e);
				}
			}
		};
		th.start();
		th.join(timeout);
		if (th.isAlive()) {
			terminateThread(th);
			throw new TimeoutException("timeout");
		}
		if (exception.get() != null)
			throw exception.get();
		return result.get();
	}
}
