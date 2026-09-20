package net.thisptr.jackson.jq.v2.ext.re2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

final class PrecompiledPatternPlan {
	private static final int MAX_VALUES = 256;

	private final List<Re2Pattern> patterns;
	private final int flagsMultiplicity;

	private PrecompiledPatternPlan(List<Re2Pattern> patterns, int flagsMultiplicity) {
		this.patterns = patterns;
		this.flagsMultiplicity = flagsMultiplicity;
	}

	List<Re2Pattern> patterns() {
		return patterns;
	}

	int flagsMultiplicity() {
		return flagsMultiplicity;
	}

	static <Context extends RuntimeContext, JsonNode> @Nullable PrecompiledPatternPlan flagsThenRegex(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpression, Expression<Context, JsonNode> flagsExpression, boolean nullableFlags) {
		return create(jsonProvider, regexExpression, flagsExpression, nullableFlags, true);
	}

	static <Context extends RuntimeContext, JsonNode> @Nullable PrecompiledPatternPlan regexThenFlags(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpression, Expression<Context, JsonNode> flagsExpression, boolean nullableFlags) {
		return create(jsonProvider, regexExpression, flagsExpression, nullableFlags, false);
	}

	private static <Context extends RuntimeContext, JsonNode> @Nullable PrecompiledPatternPlan create(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpression, Expression<Context, JsonNode> flagsExpression, boolean nullableFlags, boolean flagsFirst) {
		List<JsonNode> regexValues = constantResults(regexExpression);
		List<JsonNode> flagsValues = constantResults(flagsExpression);
		if (regexValues == null || flagsValues == null || regexValues.size() > MAX_VALUES || flagsValues.size() > MAX_VALUES || exceedsProductLimit(regexValues.size(), flagsValues.size()))
			return null;

		List<Re2Pattern> patterns = new ArrayList<>(regexValues.size() * flagsValues.size());
		if (flagsFirst) {
			for (JsonNode flags : flagsValues)
				for (JsonNode regex : regexValues)
					patterns.add(compile(jsonProvider, regex, flags, nullableFlags));
		} else {
			for (JsonNode regex : regexValues)
				for (JsonNode flags : flagsValues)
					patterns.add(compile(jsonProvider, regex, flags, nullableFlags));
		}
		return new PrecompiledPatternPlan(Collections.unmodifiableList(patterns), flagsValues.size());
	}

	private static <Context extends RuntimeContext, JsonNode> @Nullable List<JsonNode> constantResults(Expression<Context, JsonNode> expression) {
		if (!(expression instanceof ConstantExpression<?, ?> constExpr))
			return null;
		@SuppressWarnings("unchecked")
		ConstantExpression<Context, JsonNode> typed = (ConstantExpression<Context, JsonNode>) constExpr;
		return typed.getConstantResults();
	}

	private static boolean exceedsProductLimit(int left, int right) {
		return left != 0 && right > MAX_VALUES / left;
	}

	private static <JsonNode> Re2Pattern compile(JsonProvider<JsonNode> jsonProvider, JsonNode regex, JsonNode flags, boolean nullableFlags) throws JsonQueryException {
		Preconditions.checkArgumentType(jsonProvider, "regex", 1, regex, JsonNodeType.STRING);
		if (nullableFlags)
			Preconditions.checkArgumentType(jsonProvider, "regex", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
		else
			Preconditions.checkArgumentType(jsonProvider, "regex", 2, flags, JsonNodeType.STRING);
		String flagsText = jsonProvider.isNull(flags) ? null : jsonProvider.getString(flags);
		return new Re2Pattern(jsonProvider.getString(regex), flagsText);
	}
}
