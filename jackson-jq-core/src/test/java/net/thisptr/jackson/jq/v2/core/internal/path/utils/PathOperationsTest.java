package net.thisptr.jackson.jq.v2.core.internal.path.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.commons.range.LongRange;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.path.IntIndexPath;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.RootPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathOperationsTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProviderImpl.getInstance();

	@Test
	void resolvesIntIndexPathWithoutChangingItsRepresentation() throws Exception {
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndex(-1);
		List<JsonNode> values = new ArrayList<>();
		List<Path<JsonNode>> paths = new ArrayList<>();

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[10,20]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
			paths.add(Objects.requireNonNull(valuePath));
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.createNumber(20));
		assertThat(paths).singleElement().isInstanceOf(IntIndexPath.class);
		assertThat(paths.get(0).toJsonList(JSON_PROVIDER)).containsExactly(JSON_PROVIDER.createNumber(-1));
	}

	@Test
	void resolvesOutOfRangeIntIndexPathToNull() throws Exception {
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndex(3);
		List<JsonNode> values = new ArrayList<>();
		List<Path<JsonNode>> paths = new ArrayList<>();

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[10,20]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
			paths.add(Objects.requireNonNull(valuePath));
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.createNull());
		assertThat(paths.get(0).toJsonList(JSON_PROVIDER)).containsExactly(JSON_PROVIDER.createNumber(3));
	}

	@Test
	void mutatesAndExtendsArraysThroughIntIndexPath() throws Exception {
		JsonNode replaced = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(-1),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);
		JsonNode extended = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(2),
				JSON_PROVIDER.parse("[1]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);

		assertThat(replaced).isEqualTo(JSON_PROVIDER.parse("[1,9]"));
		assertThat(extended).isEqualTo(JSON_PROVIDER.parse("[1,null,9]"));
	}

	@Test
	void rejectsInvalidIntIndexPathMutations() throws Exception {
		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(-3),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2))
				.hasMessage("Out of bounds negative array index");

		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(0),
				JSON_PROVIDER.createBoolean(false),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2))
				.hasMessageContaining("Cannot index boolean with number");
	}

	@Test
	void mutatesAndExtendsArraysThroughNumberIndexPath() throws Exception {
		JsonNode replaced = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(JSON_PROVIDER, JSON_PROVIDER.createNumber(-1)),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);
		JsonNode extended = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(JSON_PROVIDER, JSON_PROVIDER.createNumber(2)),
				JSON_PROVIDER.parse("[1]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);

		assertThat(replaced).isEqualTo(JSON_PROVIDER.parse("[1,9]"));
		assertThat(extended).isEqualTo(JSON_PROVIDER.parse("[1,null,9]"));
	}

	@Test
	void rejectsInvalidNumberIndexPathMutations() throws Exception {
		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(JSON_PROVIDER, JSON_PROVIDER.createNumber(-3)),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2))
				.hasMessage("Out of bounds negative array index");

		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(JSON_PROVIDER, JSON_PROVIDER.createNumber(Double.NaN)),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2))
				.hasMessage("Cannot use nan as array index");

		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(JSON_PROVIDER, JSON_PROVIDER.createNumber(Double.POSITIVE_INFINITY)),
				JSON_PROVIDER.parse("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2))
				.hasMessage("Cannot use infinite as array index");
	}

	@Test
	void mutatesObjectFieldsPreservingFieldOrder() throws Exception {
		JsonNode updated = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendKey("b"),
				JSON_PROVIDER.parse("{\"a\":1,\"b\":2,\"c\":3}"),
				oldValue -> JSON_PROVIDER.createNumber(20),
				Versions.JQ_1_8_2);

		assertThat(JSON_PROVIDER.format(updated)).isEqualTo("{\"a\":1,\"b\":20,\"c\":3}");
	}

	@Test
	void mutatesObjectFieldsAppendingNewKeys() throws Exception {
		JsonNode extended = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendKey("d"),
				JSON_PROVIDER.parse("{\"a\":1,\"b\":2}"),
				oldValue -> {
					// A location that holds nothing yet is presented as a JSON null, never as Java null.
					assertThat(JSON_PROVIDER.getNodeType(oldValue)).isEqualTo(JsonNodeType.NULL);
					return JSON_PROVIDER.createNumber(4);
				},
				Versions.JQ_1_8_2);

		assertThat(JSON_PROVIDER.format(extended)).isEqualTo("{\"a\":1,\"b\":2,\"d\":4}");
	}

	@Test
	void mutatesArraySliceWithShorterReplacement() throws Exception {
		List<JsonNode> capturedOldSlice = new ArrayList<>();
		JsonNode replacement = JSON_PROVIDER.parse("[9]");

		JsonNode result = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndexRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(1), JSON_PROVIDER.createNumber(4)),
				JSON_PROVIDER.parse("[0,1,2,3,4]"),
				oldValue -> {
					capturedOldSlice.add(oldValue);
					return replacement;
				},
				Versions.JQ_1_8_2);

		assertThat(capturedOldSlice).containsExactly(JSON_PROVIDER.parse("[1,2,3]"));
		assertThat(result).isEqualTo(JSON_PROVIDER.parse("[0,9,4]"));
	}

	@Test
	void mutatesArraySliceWithLongerReplacement() throws Exception {
		JsonNode replacement = JSON_PROVIDER.parse("[8,9,10]");

		JsonNode result = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndexRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(1), JSON_PROVIDER.createNumber(2)),
				JSON_PROVIDER.parse("[0,1,2]"),
				oldValue -> replacement,
				Versions.JQ_1_8_2);

		assertThat(result).isEqualTo(JSON_PROVIDER.parse("[0,8,9,10,2]"));
	}

	@Test
	void resolvesArrayRangeIndexClampingOutOfBounds() throws Exception {
		List<JsonNode> values = new ArrayList<>();
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndexRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(1), JSON_PROVIDER.createNumber(10));

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[0,1,2]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.parse("[1,2]"));
	}

	@Test
	void resolvesEmptyArrayRangeIndex() throws Exception {
		List<JsonNode> values = new ArrayList<>();
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndexRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(5), JSON_PROVIDER.createNumber(10));

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[0,1,2]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.parse("[]"));
	}

	@Test
	void resolvesArrayIndexOfAllMatches() throws Exception {
		List<JsonNode> values = new ArrayList<>();
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndexOf(JSON_PROVIDER, JSON_PROVIDER.parse("[1,2]"));

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[1,2,3,1,2]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.parse("[0,3]"));
	}

	@Test
	void resolvesArrayIndexOfNoMatches() throws Exception {
		List<JsonNode> values = new ArrayList<>();
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndexOf(JSON_PROVIDER, JSON_PROVIDER.parse("[9,9]"));

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[1,2,3]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.parse("[]"));
	}

	@Test
	void resolvesArrayIndexOfEmptySubsequenceToEmptyResult() throws Exception {
		List<JsonNode> values = new ArrayList<>();
		Path<JsonNode> path = RootPath.<JsonNode>getInstance().appendIndexOf(JSON_PROVIDER, JSON_PROVIDER.parse("[]"));

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.parse("[1,2,3]"), RootPath.getInstance(), (value, valuePath) -> {
			values.add(value);
		}, false, Versions.JQ_1_8_2);

		assertThat(values).containsExactly(JSON_PROVIDER.parse("[]"));
	}

	@Test
	void testResolveRange() {
		// normal range [1, 3)
		LongRange r1 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(1), JSON_PROVIDER.createNumber(3), 5);
		assertThat(r1).isEqualTo(LongRange.of(1, 3));

		// null start defaults to 0, null end defaults to size
		LongRange r2 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNull(), JSON_PROVIDER.createNull(), 5);
		assertThat(r2).isEqualTo(LongRange.of(0, 5));

		// negative indices offset from size: [-3, -1) on size 5 -> [2, 4)
		LongRange r3 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(-3), JSON_PROVIDER.createNumber(-1), 5);
		assertThat(r3).isEqualTo(LongRange.of(2, 4));

		// start beyond size clamped to [size, size)
		LongRange r4 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(10), JSON_PROVIDER.createNumber(12), 5);
		assertThat(r4).isEqualTo(LongRange.of(5, 5));

		// negative start clamped to 0
		LongRange r5 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(-10), JSON_PROVIDER.createNumber(3), 5);
		assertThat(r5).isEqualTo(LongRange.of(0, 3));

		// end beyond size clamped to size
		LongRange r6 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(2), JSON_PROVIDER.createNumber(10), 5);
		assertThat(r6).isEqualTo(LongRange.of(2, 5));

		// inverted range (start > end) returns empty range at start
		LongRange r7 = PathOperations.resolveRange(JSON_PROVIDER, JSON_PROVIDER.createNumber(4), JSON_PROVIDER.createNumber(2), 5);
		assertThat(r7).isEqualTo(LongRange.of(4, 4));
	}
}
