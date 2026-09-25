package net.thisptr.jackson.jq.v2.core.internal.typecheck;

import java.util.Map;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.ArrayMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.ObjectMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.PatternMatcher;
import net.thisptr.jackson.jq.v2.core.internal.tree.matcher.ValueMatcher;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.UnionType;

/**
 * Works out what a destructuring pattern binds each of its variables to.
 * <p>
 * Dispatch goes through {@link PatternMatcher.Visitor} so that a new kind of pattern cannot quietly reach
 * this analysis and be bound to {@code ANY}: it would not compile until a rule for it exists.
 * <p>
 * The rules follow the matchers themselves. An array pattern reaches into an array or a null; an object
 * pattern evaluates each key expression against the object being matched, requires a string, and supplies
 * null for an absent key ({@code ObjectMatcher}'s {@code recursive}). A value no pattern of that shape can
 * match is a runtime error, so it is a type error here.
 *
 * @param <JsonNode> the JSON node type
 */
final class PatternTypes<JsonNode> implements PatternMatcher.Visitor<JsonNode, Void> {
	private final TypeCheck check;
	private final Map<Integer, Type> bindings;
	private @Var Type matched;

	private PatternTypes(TypeCheck check, Map<Integer, Type> bindings, Type matched) {
		this.check = check;
		this.bindings = bindings;
		this.matched = matched;
	}

	/**
	 * Adds to {@code bindings} the type each variable the pattern binds takes when matched against
	 * {@code value}. A variable bound at more than one position takes the union of them.
	 */
	static <N> void bind(TypeCheck check, PatternMatcher<N> matcher, Type value, Map<Integer, Type> bindings) {
		matcher.accept(new PatternTypes<>(check, bindings, value));
	}

	@Override
	public Void visit(ValueMatcher<JsonNode> matcher) {
		if (matcher.slot() >= 0)
			bindings.merge(matcher.slot(), matched, (alternatives, alternatives2) -> UnionType.of(alternatives, alternatives2));
		return null;
	}

	@Override
	public Void visit(ArrayMatcher<JsonNode> matcher) {
		Type container = matched;
		for (int i = 0; i < matcher.matchers().size(); i++) {
			matched = TypeRelations.arrayPatternElement(container, i);
			matcher.matchers().get(i).accept(this);
		}
		matched = container;
		return null;
	}

	@Override
	public Void visit(ObjectMatcher<JsonNode> matcher) {
		Type container = matched;
		for (ObjectMatcher.FieldMatcher<JsonNode> field : matcher.matchers()) {
			Type keyType = check.infer(field.name(), container);
			TypeCheck.require(StringType.getInstance(), keyType, "Object pattern key must be a string, not " + keyType);
			Type value = TypeRelations.objectPatternValue(container, TypeCheck.stringLiteral(field.name()));
			if (field.dollar() && field.writeSlot() >= 0)
				bindings.merge(field.writeSlot(), value, (alternatives, alternatives2) -> UnionType.of(alternatives, alternatives2));
			PatternMatcher<JsonNode> nested = field.matcher();
			if (nested != null) {
				matched = value;
				nested.accept(this);
			}
		}
		matched = container;
		return null;
	}
}
