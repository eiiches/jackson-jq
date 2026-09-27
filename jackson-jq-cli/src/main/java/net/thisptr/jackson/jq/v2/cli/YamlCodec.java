package net.thisptr.jackson.jq.v2.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import net.thisptr.jackson.jq.v2.json.JsonProvider;

/**
 * Converts YAML documents at the CLI boundary, independently of the selected JSON provider.
 */
final class YamlCodec {
	private static final ObjectMapper JSON = new ObjectMapper()
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
	private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()
			.enable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

	private YamlCodec() {
	}

	static <N> void read(JsonProvider<N> provider, InputStream stream, Consumer<N> consumer) {
		try (JsonParser parser = YAML.createParser(stream)) {
			for (JsonNode document = YAML.readTree(parser); document != null; document = YAML.readTree(parser)) {
				consumer.accept(provider.parse(document.toString()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static <N> String print(JsonProvider<N> provider, N node) {
		try {
			return YAML.writeValueAsString(JSON.readTree(provider.format(node)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
