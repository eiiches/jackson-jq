package net.thisptr.jackson.jq.v2.test.random;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;

public class RandomGenerator implements Generator {
	private final Function<List<AstNode>, AstNode> fn;
	private final int minArgs;
	private final int maxArgs;

	public RandomGenerator(int args, java.util.function.Function<List<AstNode>, AstNode> fn) {
		this(args, args, fn);
	}

	public RandomGenerator(int minArgs, int maxArgs, java.util.function.Function<List<AstNode>, AstNode> fn) {
		this.minArgs = minArgs;
		this.maxArgs = maxArgs;
		this.fn = fn;
	}

	@Override
	public int args() {
		return ThreadLocalRandom.current().nextInt(minArgs, maxArgs + 1);
	}

	@Override
	public AstNode generate(List<AstNode> expressions) {
		return fn.apply(expressions);
	}
}
