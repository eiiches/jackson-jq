package net.thisptr.jackson.jq.v2.regex.impl.joni;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.ConstantExpression;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

final class PrecompiledPatternPlan {
	private static final int MAX_VALUES = 256;

	private final List<OnigUtils.Pattern> patterns;
	private final int flagsMultiplicity;

	private PrecompiledPatternPlan(List<OnigUtils.Pattern> patterns, int flagsMultiplicity) {
		this.patterns = patterns;
		this.flagsMultiplicity = flagsMultiplicity;
	}

	public List<OnigUtils.Pattern> patterns() {
		return patterns;
	}

	public int flagsMultiplicity() {
		return flagsMultiplicity;
	}

	public static <Context, JsonNode> @Nullable PrecompiledPatternPlan flagsThenRegex(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpr, Expression<Context, JsonNode> flagsExpr, boolean nullableFlags) {
		return create(jsonProvider, regexExpr, flagsExpr, nullableFlags, true);
	}

	public static <Context, JsonNode> @Nullable PrecompiledPatternPlan regexThenFlags(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpr, Expression<Context, JsonNode> flagsExpr, boolean nullableFlags) {
		return create(jsonProvider, regexExpr, flagsExpr, nullableFlags, false);
	}

	private static <Context, JsonNode> @Nullable PrecompiledPatternPlan create(JsonProvider<JsonNode> jsonProvider, Expression<Context, JsonNode> regexExpr, Expression<Context, JsonNode> flagsExpr, boolean nullableFlags, boolean flagsFirst) {
		List<JsonNode> regexValues = constantResults(regexExpr);
		List<JsonNode> flagsValues = constantResults(flagsExpr);
		if (regexValues == null || flagsValues == null || regexValues.size() > MAX_VALUES || flagsValues.size() > MAX_VALUES || exceedsProductLimit(regexValues.size(), flagsValues.size()))
			return null;

		List<OnigUtils.Pattern> patterns = new ArrayList<>(regexValues.size() * flagsValues.size());
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

	private static <JsonNode> @Nullable List<JsonNode> constantResults(Expression<?, JsonNode> expression) {
		if (!(expression instanceof ConstantExpression<?, ?>))
			return null;
		return ((ConstantExpression<?, JsonNode>) expression).getConstantResults();
	}

	private static boolean exceedsProductLimit(int left, int right) {
		return left != 0 && right > MAX_VALUES / left;
	}

	private static <JsonNode> OnigUtils.Pattern compile(JsonProvider<JsonNode> jsonProvider, JsonNode regex, JsonNode flags, boolean nullableFlags) throws JsonQueryException {
		Preconditions.checkArgumentType(jsonProvider, "regex", 1, regex, JsonNodeType.STRING);
		if (nullableFlags)
			Preconditions.checkArgumentType(jsonProvider, "regex", 2, flags, JsonNodeType.STRING, JsonNodeType.NULL);
		else
			Preconditions.checkArgumentType(jsonProvider, "regex", 2, flags, JsonNodeType.STRING);
		String flagsText = jsonProvider.isNull(flags) ? null : jsonProvider.getString(flags);
		return new OnigUtils.Pattern(jsonProvider.getString(regex), flagsText);
	}
}
