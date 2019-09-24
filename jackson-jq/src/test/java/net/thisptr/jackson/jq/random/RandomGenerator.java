package net.thisptr.jackson.jq.random;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import net.thisptr.jackson.jq.Expression;

public class RandomGenerator implements Generator {
	private final Function<List<Expression>, Expression> fn;
	private final int minArgs;
	private final int maxArgs;

	public RandomGenerator(final int args, final java.util.function.Function<List<Expression>, Expression> fn) {
		this(args, args, fn);
	}

	public RandomGenerator(final int minArgs, final int maxArgs, final java.util.function.Function<List<Expression>, Expression> fn) {
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.fn = fn;
	}

	@Override
	public int args() {
		return ThreadLocalRandom.current().nextInt(minArgs, maxArgs + 1);
	}

	@Override
	public Expression generate(final List<Expression> expressions) {
		return fn.apply(expressions);
	}
}
