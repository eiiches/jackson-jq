package examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

import net.thisptr.jackson.jq.v2.core.CompileOptions;
import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.diagnostic.Diagnostic;
import net.thisptr.jackson.jq.v2.core.diagnostic.SourceLocation;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagnosticListenerTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void receivesCompileDiagnostics() {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_8_2).build();
		String source = "1, 2 | .";
		List<Diagnostic> diagnostics = new ArrayList<>();
		CompileOptions options = CompileOptions.newBuilder()
				.setDiagnosticListener(diagnostics::add)
				.build();

		JsonQuery<JsonNode> query = environment.compile(source, options);
		assertThat(diagnostics).singleElement().satisfies(diagnostic -> {
			assertThat(diagnostic.severity()).isEqualTo(Diagnostic.Severity.WARNING);
			assertThat(diagnostic.message()).contains("`,` binds tighter than `|`");
			SourceLocation location = Objects.requireNonNull(diagnostic.location());
			assertThat(location).isEqualTo(SourceLocation.of(1, 1, 1, 4));
		});

		JsonNode input = NullNode.getInstance();
		List<JsonNode> output = query.apply(input);
		assertThat(output).extracting(JsonNode::intValue).containsExactly(1, 2);
	}
}
