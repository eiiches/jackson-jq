package net.thisptr.jackson.jq.v2.spi.path;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathTest {
	@Test
	void serializesEveryRepresentablePathType() throws Exception {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());
		@Var Path<JsonNode> path = RootPath.getInstance();
		path = StringKeyPath.of(path, "a");
		path = NumberIndexPath.of(jsonProvider, path, jsonProvider.createNumber(2));
		path = IntIndexPath.of(path, 4);
		path = IndexRangePath.of(jsonProvider, path, jsonProvider.createNull(), jsonProvider.createNumber(3));
		JsonNode searchSequence = jsonProvider.createArray(Collections.singletonList(jsonProvider.createString("x")));
		path = IndexOfPath.of(jsonProvider, path, searchSequence);

		List<JsonNode> result = path.toJsonList(jsonProvider);

		assertThat(result).containsExactly(
				jsonProvider.createString("a"),
				jsonProvider.createNumber(2),
				jsonProvider.createNumber(4),
				jsonProvider.parse("{\"start\":null,\"end\":3}"),
				searchSequence);
		assertThat(((IndexOfPath<JsonNode>) path).getSearchSequence()).isSameAs(searchSequence);
	}

	@Test
	void rejectsInvalidPath() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());
		Path<JsonNode> path = InvalidPath.of(RootPath.getInstance(), jsonProvider.createBoolean(false));

		assertThatThrownBy(() -> path.toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void rejectsInvalidArrayRangeBounds() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());
		Path<JsonNode> parent = RootPath.getInstance();
		JsonNode nullNode = jsonProvider.createNull();
		JsonNode invalidBound = jsonProvider.createString("invalid");

		assertThatThrownBy(() -> IndexRangePath.of(jsonProvider, parent, invalidBound, nullNode))
				.isInstanceOf(Exception.class)
				.hasMessage("Start and end indices of an array slice must be numbers");
		assertThatThrownBy(() -> IndexRangePath.of(jsonProvider, parent, nullNode, invalidBound))
				.isInstanceOf(Exception.class)
				.hasMessage("Start and end indices of an array slice must be numbers");
	}

	@Test
	void rejectsInvalidArrayIndexOfSearchSequence() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());

		assertThatThrownBy(() -> IndexOfPath.of(jsonProvider, RootPath.getInstance(), jsonProvider.createString("invalid")))
				.isInstanceOf(Exception.class)
				.hasMessage("Array index-of search sequence must be an array");
	}

	@Test
	void rejectsLostPath() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());

		assertThatThrownBy(() -> UnrepresentablePath.<JsonNode>getInstance().toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void rejectsUntrackedPath() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());

		assertThatThrownBy(() -> UntrackedPath.<JsonNode>getInstance().toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}

	@Test
	void untrackedPathIgnoresEveryChainStep() {
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());
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
		Jackson2JsonProviderImpl jsonProvider = new Jackson2JsonProviderImpl(new ObjectMapper());
		Path<JsonNode> parent = RootPath.<JsonNode>getInstance().appendKey("a");

		assertThat(parent.appendKey("b").toJsonList(jsonProvider))
				.isEqualTo(StringKeyPath.of(parent, "b").toJsonList(jsonProvider));
		assertThat(parent.appendIndex(1).toJsonList(jsonProvider))
				.isEqualTo(IntIndexPath.of(parent, 1).toJsonList(jsonProvider));
		assertThat(parent.appendIndex(jsonProvider, jsonProvider.createNumber(1)).toJsonList(jsonProvider))
				.isEqualTo(NumberIndexPath.of(jsonProvider, parent, jsonProvider.createNumber(1)).toJsonList(jsonProvider));
		assertThat(parent.appendIndexRange(jsonProvider, jsonProvider.createNull(), jsonProvider.createNumber(1)).toJsonList(jsonProvider))
				.isEqualTo(IndexRangePath.of(jsonProvider, parent, jsonProvider.createNull(), jsonProvider.createNumber(1)).toJsonList(jsonProvider));
		JsonNode searchSequence = jsonProvider.createArray(Collections.emptyList());
		assertThat(parent.appendIndexOf(jsonProvider, searchSequence).toJsonList(jsonProvider))
				.isEqualTo(IndexOfPath.of(jsonProvider, parent, searchSequence).toJsonList(jsonProvider));
		assertThatThrownBy(() -> parent.appendInvalid(jsonProvider.createBoolean(false)).toJsonList(jsonProvider))
				.isInstanceOf(Exception.class)
				.hasMessage("Invalid path expression");
	}
}
