package net.thisptr.jackson.jq.v2.spi.path;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.json.internal.io.JsonCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathTest {
	@Test
	void serializesEveryRepresentablePathType() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());
		@Var Path<JsonNode> path = RootPath.getInstance();
		path = StringKeyPath.of(path, "a");
		path = NumberIndexPath.of(jsonProvider, path, jsonProvider.createNumber(2));
		path = IntIndexPath.of(path, 4);
		path = IndexRangePath.of(path, jsonProvider.createNull(), jsonProvider.createNumber(3));
		JsonNode searchSequence = jsonProvider.createArray(Collections.singletonList(jsonProvider.createString("x")));
		path = IndexOfPath.of(jsonProvider, path, searchSequence);

		List<JsonNode> result = path.toJsonList(jsonProvider);

		// Built rather than parsed: the path carries the very nodes it was given, and a parsed 3 keeps the
		// literal it was parsed from, so it is not the same node as the int one above.
		Map<String, JsonNode> expectedRange = new LinkedHashMap<>();
		expectedRange.put("start", jsonProvider.createNull());
		expectedRange.put("end", jsonProvider.createNumber(3));

		assertThat(result).containsExactly(
				jsonProvider.createString("a"),
				jsonProvider.createNumber(2),
				jsonProvider.createNumber(4),
				jsonProvider.createObject(expectedRange),
				searchSequence);
		assertThat(((IndexOfPath<JsonNode>) path).getSearchSequence()).isSameAs(searchSequence);
	}

	@Test
	void rejectsInvalidPath() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());
		Path<JsonNode> path = InvalidPath.of(RootPath.getInstance(), jsonProvider.createBoolean(false));

		assertThatThrownBy(() -> path.toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void serializesNonNumericRangeBounds() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());
		Path<JsonNode> parent = RootPath.getInstance();
		JsonNode start = jsonProvider.createString("start");
		JsonNode end = jsonProvider.createBoolean(false);

		assertThat(IndexRangePath.of(parent, start, end).toJsonList(jsonProvider))
				.containsExactly(JsonCodec.parse(jsonProvider, "{\"start\":\"start\",\"end\":false}"));
	}

	@Test
	void rejectsInvalidArrayIndexOfSearchSequence() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());

		assertThatThrownBy(() -> IndexOfPath.of(jsonProvider, RootPath.getInstance(), jsonProvider.createString("invalid")))
				.isInstanceOf(Exception.class)
				.hasMessage("Array index-of search sequence must be an array");
	}

	@Test
	void rejectsLostPath() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());

		assertThatThrownBy(() -> UnrepresentablePath.of(RootPath.<JsonNode>getInstance(), jsonProvider.createNull()).toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void stalePathKeepsThePositionItWasLostAt() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());

		Path<JsonNode> lastValid = RootPath.<JsonNode>getInstance().appendKey("a");
		JsonNode valueAtLastValid = jsonProvider.createNull();
		Path<JsonNode> stale = UnrepresentablePath.of(lastValid, valueAtLastValid);

		assertThat(stale).isInstanceOf(UnrepresentablePath.class);
		assertThat(((UnrepresentablePath<JsonNode>) stale).getLastValidPath()).isSameAs(lastValid);
		assertThat(((UnrepresentablePath<JsonNode>) stale).getValueAtLastValidPath()).isSameAs(valueAtLastValid);
		assertThat(stale.getParentPath()).isNull();
		// A path that already went stale keeps the position it first lost.
		assertThat(UnrepresentablePath.of(stale, jsonProvider.createNumber(1))).isSameAs(stale);
	}

	@Test
	void rejectsUntrackedPath() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());

		assertThatThrownBy(() -> UntrackedPath.<JsonNode>getInstance().toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void untrackedPathIgnoresEveryChainStep() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());
		UntrackedPath<JsonNode> untracked = UntrackedPath.getInstance();

		assertThat(untracked.appendKey("a")).isSameAs(untracked);
		assertThat(untracked.appendIndex(1)).isSameAs(untracked);
		assertThat(untracked.appendIndex(jsonProvider, jsonProvider.createNumber(1))).isSameAs(untracked);
		assertThat(untracked.appendIndexRange(jsonProvider, jsonProvider.createNull(), jsonProvider.createNumber(1))).isSameAs(untracked);
		assertThat(untracked.appendIndexOf(jsonProvider, jsonProvider.createArray(Collections.emptyList()))).isSameAs(untracked);
		assertThat(untracked.appendInvalid(jsonProvider.createBoolean(false))).isSameAs(untracked);
	}

	@Test
	void defaultChainMethodsMatchTheirStaticFactories() {
		Jackson2JsonProvider jsonProvider = new Jackson2JsonProvider(new ObjectMapper());
		Path<JsonNode> parent = RootPath.<JsonNode>getInstance().appendKey("a");

		assertThat(parent.appendKey("b").toJsonList(jsonProvider))
				.isEqualTo(StringKeyPath.of(parent, "b").toJsonList(jsonProvider));
		assertThat(parent.appendIndex(1).toJsonList(jsonProvider))
				.isEqualTo(IntIndexPath.of(parent, 1).toJsonList(jsonProvider));
		assertThat(parent.appendIndex(jsonProvider, jsonProvider.createNumber(1)).toJsonList(jsonProvider))
				.isEqualTo(NumberIndexPath.of(jsonProvider, parent, jsonProvider.createNumber(1)).toJsonList(jsonProvider));
		assertThat(parent.appendIndexRange(jsonProvider, jsonProvider.createNull(), jsonProvider.createNumber(1)).toJsonList(jsonProvider))
				.isEqualTo(IndexRangePath.of(parent, jsonProvider.createNull(), jsonProvider.createNumber(1)).toJsonList(jsonProvider));
		JsonNode searchSequence = jsonProvider.createArray(Collections.emptyList());
		assertThat(parent.appendIndexOf(jsonProvider, searchSequence).toJsonList(jsonProvider))
				.isEqualTo(IndexOfPath.of(jsonProvider, parent, searchSequence).toJsonList(jsonProvider));
		assertThatThrownBy(() -> parent.appendInvalid(jsonProvider.createBoolean(false)).toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}
}
