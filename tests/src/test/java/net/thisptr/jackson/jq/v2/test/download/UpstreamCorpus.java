package net.thisptr.jackson.jq.v2.test.download;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.test.testcase.VersionSpelling;

/**
 * jq's own test corpora, read from the release tag that published them.
 *
 * <p>Three sources of two shapes: {@code tests/jq.test} and {@code tests/onig.test} are records of a
 * program, an input and the output lines it is expected to print, separated by blank lines, while the
 * manual carries its examples as YAML. The expected output either declares is only ever
 * cross-checked -- the expectations a case ends up asserting come from running jq, not from reading
 * them -- so a source that spells one unreadably still yields a usable case.
 */
public final class UpstreamCorpus {
	private static final String RAW = "https://raw.githubusercontent.com/jqlang/jq/";

	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	/**
	 * Reads an input, which the corpus writes out again, keeping the number spelling it was written
	 * with the way {@code TestCaseLoader} does.
	 */
	private static final ObjectMapper INPUT_MAPPER = JsonMapper.builder()
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
			.build();

	/**
	 * Reads a declared output, which is only ever compared against what jq printed, so it is read
	 * the way {@code JqRunner} reads jq's stdout rather than the way the corpus is written.
	 */
	private static final ObjectMapper OUTPUT_MAPPER = new ObjectMapper();

	private static final YAMLMapper YAML_MAPPER = YAMLMapper.builder().build();

	/**
	 * Which of jq's corpora a case came from, and the file each golden file is named after.
	 */
	public enum Kind {
		JQ_TEST("", "tests/jq.test"),
		ONIG("-onig", "tests/onig.test"),
		MANUAL("-manual", null);

		private final String suffix;
		private final @Nullable String testPath;

		Kind(String suffix, @Nullable String testPath) {
			this.suffix = suffix;
			this.testPath = testPath;
		}

		/**
		 * The golden file this corpus is imported into, e.g. {@code jq-1.8.2-onig.yaml}.
		 */
		public String fileName(Version version) {
			return "jq-" + VersionSpelling.of(version) + suffix + ".yaml";
		}

		private String path(Version version) {
			if (testPath != null)
				return testPath;
			// The manual left its numbered directory in 1.7.
			return version.compareTo(Version.of(1, 7, 0)) < 0
					? "docs/content/3.manual/manual.yml"
					: "docs/content/manual/manual.yml";
		}
	}

	/**
	 * One case as a corpus declares it.
	 *
	 * @param program the jq program
	 * @param input the single input it is run on
	 * @param declaresFailure whether the corpus declares that jq rejects the program
	 * @param declaredOutput what the corpus says jq prints, or null when it spells it unreadably
	 * @param origin the tag, file and line the case was read from
	 */
	public record UpstreamCase(
			String program,
			JsonNode input,
			boolean declaresFailure,
			@Nullable List<JsonNode> declaredOutput,
			String origin) {
	}

	/**
	 * A case left out because its input is not JSON. jq's corpus writes a few with a NaN or an
	 * infinity in them, which neither Jackson reads nor the golden format has a spelling for.
	 *
	 * @param program the jq program
	 * @param input the input line as the corpus writes it
	 * @param origin the tag, file and line it was read from
	 */
	public record Unreadable(String program, String input, String origin) {
	}

	/**
	 * One corpus as it was read.
	 *
	 * @param cases the cases it declares, in the order it writes them
	 * @param unreadable the cases whose input could not be read
	 */
	public record Corpus(List<UpstreamCase> cases, List<Unreadable> unreadable) {
	}

	/**
	 * Fetches and parses one of jq's corpora at one release.
	 *
	 * @param kind which corpus to read
	 * @param version the release whose tag to read it from
	 * @return the cases it declares
	 * @throws IOException if the corpus cannot be fetched or is not the shape it should be
	 * @throws InterruptedException if the fetch is interrupted
	 */
	public static Corpus fetch(Kind kind, Version version) throws IOException, InterruptedException {
		@Var String path = kind.path(version);
		@Var String text = get(version, path);
		if (kind == Kind.MANUAL) {
			String target = text.strip();
			// At the 1.8.x tags the manual is a symlink, whose raw body is the single line naming
			// its target rather than the manual itself.
			if (!target.contains("\n") && target.endsWith(".yml")) {
				path = path.substring(0, path.lastIndexOf('/') + 1) + target;
				text = get(version, path);
			}
		}
		String origin = "jq-" + VersionSpelling.of(version) + "/" + path;
		return kind == Kind.MANUAL ? parseManual(text, origin) : parseTests(text, origin);
	}

	private static String get(Version version, String path) throws IOException, InterruptedException {
		URI uri = URI.create(RAW + "jq-" + VersionSpelling.of(version) + "/" + path);
		HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(uri).GET().build(),
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200)
			throw new IOException(uri + ": HTTP " + response.statusCode());
		return response.body();
	}

	/**
	 * Reads {@code jq.test} or {@code onig.test}: a program, an input and the output lines it prints,
	 * one record per blank-line-separated block, plus {@code %%FAIL} blocks of a program and the
	 * error jq is expected to print instead.
	 */
	private static Corpus parseTests(String text, String origin) {
		List<String> lines = text.lines().toList();
		List<UpstreamCase> cases = new ArrayList<>();
		List<Unreadable> unreadable = new ArrayList<>();
		@Var int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);
			if (line.isBlank() || line.stripLeading().startsWith("#")) {
				i++;
				continue;
			}
			int lineNumber = i + 1;
			boolean declaresFailure = line.startsWith("%%FAIL");
			if (declaresFailure)
				i++;
			if (i >= lines.size())
				break;
			String program = lines.get(i++);
			if (declaresFailure) {
				// The rest of the block is the error jq prints, which nothing here compares.
				while (i < lines.size() && !lines.get(i).isBlank())
					i++;
				cases.add(new UpstreamCase(program, NullNode.getInstance(), true, null, origin + ":" + lineNumber));
				continue;
			}
			if (i >= lines.size())
				break;
			String inputLine = lines.get(i++);
			List<String> outputLines = new ArrayList<>();
			while (i < lines.size() && !lines.get(i).isBlank() && !lines.get(i).stripLeading().startsWith("#"))
				outputLines.add(lines.get(i++));

			JsonNode input;
			try {
				input = readInput(inputLine);
			} catch (JacksonException unreadableInput) {
				unreadable.add(new Unreadable(program, inputLine, origin + ":" + lineNumber));
				continue;
			}
			cases.add(new UpstreamCase(program, input, false, readOutput(outputLines), origin + ":" + lineNumber));
		}
		return new Corpus(cases, unreadable);
	}

	/** Reads the manual's {@code sections[].entries[].examples[]}. */
	private static Corpus parseManual(String text, String origin) throws IOException {
		JsonNode manual = YAML_MAPPER.readTree(text);
		if (!manual.path("sections").isArray())
			throw new IOException(origin + ": no sections");
		List<UpstreamCase> cases = new ArrayList<>();
		List<Unreadable> unreadable = new ArrayList<>();
		for (JsonNode section : manual.path("sections")) {
			for (JsonNode entry : section.path("entries")) {
				for (JsonNode example : entry.path("examples")) {
					String program = example.path("program").asText("");
					JsonNode input;
					try {
						input = value(example.path("input"), INPUT_MAPPER);
					} catch (JacksonException unreadableInput) {
						unreadable.add(new Unreadable(program, example.path("input").toString(), origin));
						continue;
					}
					cases.add(new UpstreamCase(program, input, false, manualOutput(example.path("output")), origin));
				}
			}
		}
		return new Corpus(cases, unreadable);
	}

	private static JsonNode readInput(String line) throws JacksonException {
		// jq's corpus writes one input with a byte order mark in front of it, to prove jq reads it;
		// nothing downstream of here can carry one, and jq is handed the value rather than the text.
		return INPUT_MAPPER.readTree(line.startsWith("\ufeff") ? line.substring(1) : line);
	}

	/** The output lines as JSON, or null when any of them is not. */
	private static @Nullable List<JsonNode> readOutput(List<String> lines) {
		List<JsonNode> values = new ArrayList<>();
		for (String line : lines) {
			try {
				values.add(OUTPUT_MAPPER.readTree(line));
			} catch (JacksonException notJson) {
				return null;
			}
		}
		return values;
	}

	/** The manual's output list as JSON, or null when any element is not. */
	private static @Nullable List<JsonNode> manualOutput(JsonNode output) {
		if (!output.isArray())
			return null;
		List<JsonNode> values = new ArrayList<>();
		for (JsonNode element : output) {
			try {
				values.add(value(element, OUTPUT_MAPPER));
			} catch (JacksonException notJson) {
				return null;
			}
		}
		return values;
	}

	/** A manual example's value: the JSON it spells as a string, or the YAML it spells directly. */
	private static JsonNode value(JsonNode node, ObjectMapper mapper) throws JacksonException {
		return node.isTextual() ? mapper.readTree(node.textValue()) : mapper.valueToTree(node);
	}

	private UpstreamCorpus() {
	}
}
