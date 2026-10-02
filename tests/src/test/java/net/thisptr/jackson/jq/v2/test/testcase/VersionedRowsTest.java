package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedRowsTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final List<Version> VERSIONS = List.of(
			Version.valueOf("1.5"), Version.valueOf("1.6"), Version.valueOf("1.7"));

	private static Map<Version, List<JsonNode>> observations(String... outputs) throws IOException {
		Map<Version, List<JsonNode>> results = new LinkedHashMap<>();
		for (int i = 0; i < outputs.length; i++)
			results.put(VERSIONS.get(i), List.of(MAPPER.readTree(outputs[i])));
		return results;
	}

	private static List<String> ranges(List<VersionedRows.Row<List<JsonNode>>> rows) {
		return rows.stream().map(VersionedRows.Row::range).toList();
	}

	@Test
	void joinsRunsWhoseOutputsOnlyDifferInHowANumberIsWritten() throws IOException {
		Map<Version, List<JsonNode>> results = observations("-1", "-1.0", "-1");
		assertThat(ranges(VersionedRows.merge(VERSIONS, results,
				(a, b) -> ExpectationComparison.sameValues(a, b, null))))
				.containsExactly("[1.5, )");
		// Without a value comparison the same observations split into rows no assertion can tell apart.
		assertThat(ranges(VersionedRows.merge(VERSIONS, results)))
				.containsExactly("[1.5, 1.6)", "[1.6, 1.7)", "[1.7, )");
	}

	@Test
	void splitsRunsWhoseOutputsDifferInValue() throws IOException {
		assertThat(ranges(VersionedRows.merge(VERSIONS, observations("-1", "-1", "-2"),
				(a, b) -> ExpectationComparison.sameValues(a, b, null))))
				.containsExactly("[1.5, 1.7)", "[1.7, )");
	}

	@Test
	void recordsTheFirstObservationOfEachRun() throws IOException {
		List<VersionedRows.Row<List<JsonNode>>> rows = VersionedRows.merge(VERSIONS, observations("-1", "-1.0", "-1.0"),
				(a, b) -> ExpectationComparison.sameValues(a, b, null));
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).value().get(0).toString()).isEqualTo("-1");
	}

	@Test
	void endsARunAtAVersionWithNoObservation() throws IOException {
		Map<Version, List<JsonNode>> results = observations("-1");
		results.put(VERSIONS.get(2), List.of(MAPPER.readTree("-1")));
		assertThat(ranges(VersionedRows.merge(VERSIONS, results,
				(a, b) -> ExpectationComparison.sameValues(a, b, null))))
				.containsExactly("[1.5, 1.6)", "[1.7, )");
	}
}
