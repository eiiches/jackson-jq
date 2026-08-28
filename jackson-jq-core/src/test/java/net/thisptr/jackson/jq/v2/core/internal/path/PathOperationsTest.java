package net.thisptr.jackson.jq.v2.core.internal.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.Versions;
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

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.fromString("[10,20]"), RootPath.getInstance(), (value, valuePath) -> {
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

		PathOperations.resolve(JSON_PROVIDER, path, JSON_PROVIDER.fromString("[10,20]"), RootPath.getInstance(), (value, valuePath) -> {
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
				JSON_PROVIDER.fromString("[1,2]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);
		JsonNode extended = PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(2),
				JSON_PROVIDER.fromString("[1]"),
				oldValue -> JSON_PROVIDER.createNumber(9),
				Versions.JQ_1_8_2);

		assertThat(replaced).isEqualTo(JSON_PROVIDER.fromString("[1,9]"));
		assertThat(extended).isEqualTo(JSON_PROVIDER.fromString("[1,null,9]"));
	}

	@Test
	void rejectsInvalidIntIndexPathMutations() throws Exception {
		assertThatThrownBy(() -> PathOperations.mutate(JSON_PROVIDER,
				RootPath.<JsonNode>getInstance().appendIndex(-3),
				JSON_PROVIDER.fromString("[1,2]"),
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
}
