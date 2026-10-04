package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestCaseFormatterTest {
	@TempDir
	Path directory;

	private String format(String yaml) throws IOException {
		Path file = Files.createTempFile(directory, "cases", ".yaml");
		Files.writeString(file, yaml, StandardCharsets.UTF_8);
		TestCaseFormatter.format(file);
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	@Test
	void writesEachFieldInItsPlaceAndItsOwnStyle() throws IOException {
		String yaml = """
				- properties:
				  - v: '[1.5, )'
				    cardinality: ONE
				    depends_on_input: false
				    depends_on_external_state: false
				  types:
				  - v: '[1.5, )'
				    input: 'ANY'
				    output: '"x"'
				  comment: 'why this case is here'
				  q: '"x"'
				  input: {"a": [1, 2.5, true, null]}
				  expectations:
				    jjq:
				    - v: '[1.5, )'
				      output: []
				      runtime_error: "example error"
				      incompat_type: INTENTIONAL
				    default:
				    - v: '[1.5, )'
				      output:
				      - "x"
				""";
		assertThat(format(yaml)).isEqualTo("""
				- q: '"x"'
				  input: {"a": [1, 2.5, true, null]}
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - "x"
				    jjq:
				    - v: '[1.5, )'
				      output: []
				      runtime_error: "example error"
				      incompat_type: INTENTIONAL
				  types:
				  - v: '[1.5, )'
				    input: 'ANY'
				    output: '"x"'
				  properties:
				  - v: '[1.5, )'
				    cardinality: ONE
				    depends_on_input: false
				    depends_on_external_state: false
				  comment: 'why this case is here'
				""");
	}

	@Test
	void keepsANumberAndAnEscapeAsTheCorpusSpellsThem() throws IOException {
		String yaml = """
				- q: '.'
				  input: [1.0e+300, 2.2250738585072014e-308, 1, 1.50]
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - "\\u03bc and \\u0101"
				""";
		assertThat(format(yaml)).isEqualTo(yaml);
	}

	@Test
	void writesDecimalsAccordingToTheirNumericSpelling() throws IOException {
		Path file = Files.createTempFile(directory, "floats", ".yaml");
		Files.writeString(file, """
				- q: '.'
				  input: [!!float '1', !!float '-2', !!float '99999999999999999999999', 1.0]
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - [!!float '0', 1, 1.50]
				""", StandardCharsets.UTF_8);
		assertThat(TestCaseFormatter.format(file)).isTrue();
		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
				- q: '.'
				  input: [1, -2, 99999999999999999999999, 1.0]
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - [0, 1, 1.50]
				""");
		TestCase testCase = TestCaseFormatter.read(file).entries().get(0).testCase();
		assertThat(testCase.input.get(0).isIntegralNumber()).isTrue();
		assertThat(testCase.input.get(1).isIntegralNumber()).isTrue();
		assertThat(testCase.input.get(2).isIntegralNumber()).isTrue();
		assertThat(testCase.input.get(3).isFloatingPointNumber()).isTrue();
		assertThat(testCase.expectations.defaultRows.get(0).values().get(0).get(0).isIntegralNumber()).isTrue();
		assertThat(testCase.expectations.defaultRows.get(0).values().get(0).get(1).isIntegralNumber()).isTrue();
		assertThat(testCase.expectations.defaultRows.get(0).values().get(0).get(2).isFloatingPointNumber()).isTrue();
		assertThat(TestCaseFormatter.isFormatted(file)).isTrue();
	}

	@Test
	void keepsVersionSpecificUnstableRows() throws IOException {
		String yaml = """
				- q: '.'
				  expectations:
				    default:
				    - v: '[1.5, 1.6)'
				      unstable: true
				    - v: '[1.6, )'
				      output:
				      - null
				""";
		assertThat(format(yaml)).isEqualTo(yaml);
	}

	@Test
	void tellsAnInputItWritesFromOneItLeavesOut() throws IOException {
		String writesNull = """
				- q: '.'
				  input: null
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - null
				""";
		assertThat(format(writesNull)).isEqualTo(writesNull);
		String omitsInput = writesNull.replace("  input: null\n", "");
		assertThat(format(omitsInput)).isEqualTo(omitsInput);
	}

	@Test
	void keepsEveryCommentWithTheCaseItWasWrittenAbove() throws IOException {
		String yaml = """
				# What this file covers, said once at the top.
				#
				# A second paragraph of the same block.
				
				# Why the first case looks like this.
				- q: '.'
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - null
				
				# --- a section of its own ---
				
				# Why the second case looks like this.
				- q: '.'
				  input: 1
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - 1
				
				# A last word, after every case.
				""";
		assertThat(format(yaml)).isEqualTo(yaml);
	}

	@Test
	void rejectsAFieldItDoesNotKnow() {
		assertThatThrownBy(() -> format("""
				- q: '.'
				  nonsense: true
				  expectations:
				    default:
				    - v: '[1.5, )'
				      output:
				      - null
				"""))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("nonsense");
	}

	@Test
	void reportsWhetherAFileChanged() throws IOException {
		Path file = Files.createTempFile(directory, "cases", ".yaml");
		Files.writeString(file, """
				- q: '.'
				  expectations:
				    default:
				    - v: "[1.5, )"
				      output: [null]
				""", StandardCharsets.UTF_8);
		assertThat(TestCaseFormatter.isFormatted(file)).isFalse();
		assertThat(TestCaseFormatter.format(file)).isTrue();
		assertThat(TestCaseFormatter.format(file)).isFalse();
		assertThat(TestCaseFormatter.isFormatted(file)).isTrue();
	}
}
