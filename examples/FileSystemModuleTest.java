package examples;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import com.google.devtools.build.runfiles.AutoBazelRepository;
import com.google.devtools.build.runfiles.Runfiles;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.module.loaders.FileSystemModuleLoader;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

@AutoBazelRepository
public class FileSystemModuleTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void loadsModuleFromFileSystem() throws IOException {
		String moduleFile = Objects.requireNonNull(Runfiles.preload()
				.withSourceRepository(AutoBazelRepository_FileSystemModuleTest.NAME)
				.rlocation("jackson_jq/examples/modules/math.jq"));
		Path moduleDirectory = Objects.requireNonNull(Paths.get(moduleFile).getParent());

		Jackson3JsonProvider jsonProvider = Jackson3JsonProvider.getInstance();
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_8_2)
				.addModuleLoader(new FileSystemModuleLoader<>(jsonProvider, moduleDirectory))
				.build();

		JsonQuery<JsonNode> query = environment.compile("import \"math\" as math; math::double");

		JsonNode input = MAPPER.readTree("21");
		List<JsonNode> output = query.apply(input);
		assertThat(output).containsExactly(IntNode.valueOf(42));
	}
}
