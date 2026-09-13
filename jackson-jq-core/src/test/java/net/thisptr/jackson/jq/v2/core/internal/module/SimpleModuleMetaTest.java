package net.thisptr.jackson.jq.v2.core.internal.module;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a module's {@code module {...};} directive and its {@code import} statements become.
 * <p>
 * Read straight off the AST rather than through a compiled module: the engine compiles modules for
 * its own use and hands them to nobody, so this is where the behaviour is observable.
 */
public class SimpleModuleMetaTest {
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	private static ModuleMeta metaOf(String source) {
		AstNode ast = AstParser.parse(source + " null", Versions.JQ_1_6);
		return SimpleModuleMeta.fromAst(ast);
	}

	private static Map<String, JsonNode> metadataOf(String source) {
		return metaOf(source).getMetadata(JSON_PROVIDER);
	}

	@Test
	public void testExposesMetadata() throws Exception {
		Map<String, JsonNode> metadata = metadataOf("module { \"author\": \"Alice\", \"version\": 1 }; def one: 1;");

		assertThat(metadata).containsKey("author");
		assertThat(Objects.requireNonNull(metadata.get("author")).asText()).isEqualTo("Alice");
		assertThat(metadata).containsKey("version");
		assertThat(Objects.requireNonNull(metadata.get("version")).asInt()).isEqualTo(1);
	}

	// Metadata is folded by ExpressionUtils.evaluateLiteralExpression, which has to walk a `,` as a
	// left-nested binary node to read an array literal's elements in order.
	@Test
	public void testFoldsArrayMetadata() throws Exception {
		Map<String, JsonNode> metadata = metadataOf("module { \"tags\": [\"a\", (\"b\", \"c\")], \"nested\": [1, [2, 3], 4], \"solo\": [\"only\"], \"none\": [] }; def one: 1;");

		assertThat(Objects.requireNonNull(metadata.get("tags")).toString()).isEqualTo("[\"a\",\"b\",\"c\"]");
		assertThat(Objects.requireNonNull(metadata.get("nested")).toString()).isEqualTo("[1,[2,3],4]");
		assertThat(Objects.requireNonNull(metadata.get("solo")).toString()).isEqualTo("[\"only\"]");
		assertThat(Objects.requireNonNull(metadata.get("none")).toString()).isEqualTo("[]");
	}

	@Test
	public void testFoldsLongArrayMetadataWithoutOverflowingTheStack() throws Exception {
		StringBuilder source = new StringBuilder("module { \"values\": [");
		for (int i = 0; i < 10_000; i++) {
			if (i != 0)
				source.append(',');
			source.append(i);
		}
		source.append("] }; def one: 1;");

		JsonNode values = Objects.requireNonNull(metadataOf(source.toString()).get("values"));

		assertThat(values).hasSize(10_000);
		assertThat(values.get(0).asInt()).isZero();
		assertThat(values.get(9_999).asInt()).isEqualTo(9_999);
	}

	@Test
	public void testExposesDependencies() throws Exception {
		List<ModuleMeta.Dependency> deps = metaOf("import \"foo/bar\" as bar; import \"data/nums\" as $nums { \"tag\": \"nums\" }; include \"helpers\"; def test: 1;")
				.getDependencies();

		assertThat(deps).hasSize(3);

		assertThat(deps.get(0).getRelpath()).isEqualTo("foo/bar");
		assertThat(deps.get(0).isData()).isFalse();
		assertThat(deps.get(0).getAlias()).isEqualTo("bar");
		assertThat(deps.get(0).getImportMetadata(JSON_PROVIDER)).isEmpty();

		assertThat(deps.get(1).getRelpath()).isEqualTo("data/nums");
		assertThat(deps.get(1).isData()).isTrue();
		assertThat(deps.get(1).getAlias()).isEqualTo("nums");
		Map<String, JsonNode> dep1Meta = deps.get(1).getImportMetadata(JSON_PROVIDER);
		assertThat(dep1Meta).containsKey("tag");
		assertThat(Objects.requireNonNull(dep1Meta.get("tag")).asText()).isEqualTo("nums");

		assertThat(deps.get(2).getRelpath()).isEqualTo("helpers");
		assertThat(deps.get(2).isData()).isFalse();
		assertThat(deps.get(2).getAlias()).isNull();
		assertThat(deps.get(2).getImportMetadata(JSON_PROVIDER)).isEmpty();
	}
}
