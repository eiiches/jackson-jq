package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.Var;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import net.thisptr.jackson.jq.v2.spi.version.Version;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

/**
 * The canonical form of the golden test cases under {@code tests/test-cases}, and the only writer
 * of it.
 *
 * <p>Every choice here is per node rather than global, which is what a plain YAML dump cannot do: a
 * query is single-quoted, a JSON value is flow style with double-quoted strings, an expectation's
 * outputs are a block sequence unless empty, and a module's source is a literal block. Each case is
 * emitted on its own and the pieces are joined a blank line apart, which is also what keeps a
 * comment with the case it was written above: SnakeYAML's emitter moves comments attached to a
 * sequence entry after the entry's own dash, so the comment blocks are carried as the text they
 * already are.
 */
public final class TestCaseFormatter {
	/**
	 * One case as the file holds it: the case itself, the comment block written above it, and whether
	 * it spells out an {@code input} whose value is null, which reads differently from leaving it out.
	 */
	public record Entry(TestCase testCase, String comment, boolean writesInput) {
	}

	/**
	 * A file's cases, and the comment block that trails the last of them.
	 */
	public record Document(List<Entry> entries, String endComment) {
	}

	/**
	 * Reads a file's cases and comments, validating the cases as {@link TestCaseLoader} does.
	 *
	 * @param file the YAML file to read
	 * @return the cases with their comments
	 * @throws IOException if the file cannot be read or is not valid test-case YAML
	 */
	public static Document read(Path file) throws IOException {
		return read(file, true);
	}

	public static Document readUnchecked(Path file) throws IOException {
		return read(file, false);
	}

	private static Document read(Path file, boolean validate) throws IOException {
		String text = Files.readString(file, StandardCharsets.UTF_8);
		List<TestCase> cases;
		try (ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
			cases = validate ? TestCaseLoader.loadTestCases(file.getFileName().toString(), in)
					: TestCaseLoader.loadTestCasesUnchecked(file.getFileName().toString(), in);
		}
		Node root = new Yaml(new LoaderOptions()).compose(new StringReader(text));
		List<Node> nodes = root instanceof SequenceNode sequence ? sequence.getValue() : List.<Node>of();
		if (nodes.size() != cases.size())
			throw new IOException(file + ": read " + cases.size() + " cases but " + nodes.size() + " nodes");

		// A case's node begins on the same line as its dash, which is where its comment block ends.
		List<String> lines = text.lines().toList();
		List<Entry> entries = new ArrayList<>();
		for (int i = 0; i < cases.size(); i++) {
			int from = i == 0 ? 0 : nodes.get(i - 1).getStartMark().getLine();
			String comment = comment(lines, from, nodes.get(i).getStartMark().getLine());
			entries.add(new Entry(cases.get(i), comment, writesInput(nodes.get(i))));
		}
		String endComment = nodes.isEmpty()
				? comment(lines, 0, lines.size())
				: comment(lines, nodes.get(nodes.size() - 1).getStartMark().getLine(), lines.size());
		return new Document(entries, endComment);
	}

	/**
	 * The comment block in {@code lines[from, to)}: whatever follows the last line of YAML there,
	 * which is how a comment belongs to the case written under it rather than the one above it.
	 */
	private static String comment(List<String> lines, int from, int to) {
		@Var int start = from;
		for (int i = from; i < to; i++) {
			String line = lines.get(i);
			if (!line.isBlank() && !line.stripLeading().startsWith("#"))
				start = i + 1;
		}
		while (start < to && lines.get(start).isBlank())
			start++;
		StringBuilder comment = new StringBuilder();
		for (int i = start; i < to; i++)
			comment.append(lines.get(i)).append('\n');
		return comment.toString();
	}

	/**
	 * Renders cases and comments as canonical YAML.
	 *
	 * @param document the cases with their comments
	 * @return the file's canonical text
	 */
	public static String render(Document document) {
		List<String> parts = new ArrayList<>();
		for (Entry entry : document.entries())
			parts.add(entry.comment() + emit(caseNode(entry.testCase(), entry.writesInput())));
		String text = parts.isEmpty() ? "[]\n" : String.join("\n", parts);
		return document.endComment().isEmpty() ? text : text + "\n" + document.endComment();
	}

	/** Emits one case as a single-entry block sequence, the way the corpus writes it. */
	private static String emit(Node caseNode) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setIndent(2);
		// format.py dumped with width=2**32 and allow_unicode=False.
		options.setWidth(Integer.MAX_VALUE);
		options.setAllowUnicode(false);
		StringWriter writer = new StringWriter();
		Serializer serializer = new Serializer(new Emitter(writer, options), new Resolver(), options, null);
		try {
			serializer.open();
			serializer.serialize(new SequenceNode(Tag.SEQ, List.of(caseNode), DumperOptions.FlowStyle.BLOCK));
			serializer.close();
		} catch (IOException unreachable) {
			throw new IllegalStateException("writing to a StringWriter cannot fail", unreachable);
		}
		return writer.toString();
	}

	/**
	 * Rewrites a file canonically.
	 *
	 * @param file the YAML file to format
	 * @return whether the file's contents changed
	 * @throws IOException if the file cannot be read or written
	 */
	public static boolean format(Path file) throws IOException {
		return write(file, read(file));
	}

	/**
	 * Writes a document to a file canonically.
	 *
	 * @param file the YAML file to write
	 * @param document the cases with their comments
	 * @return whether the file's contents changed
	 * @throws IOException if the file cannot be read or written
	 */
	public static boolean write(Path file, Document document) throws IOException {
		String formatted = render(document);
		if (formatted.equals(Files.readString(file, StandardCharsets.UTF_8)))
			return false;
		Files.writeString(file, formatted, StandardCharsets.UTF_8);
		return true;
	}

	/**
	 * Reports whether a file is already canonical, without writing it.
	 *
	 * @param file the YAML file to check
	 * @return whether the file is formatted
	 * @throws IOException if the file cannot be read
	 */
	public static boolean isFormatted(Path file) throws IOException {
		return render(read(file)).equals(Files.readString(file, StandardCharsets.UTF_8));
	}

	/** Whether the case as written spells out its {@code input} key. */
	private static boolean writesInput(Node node) {
		if (!(node instanceof MappingNode mapping))
			return false;
		for (NodeTuple field : mapping.getValue()) {
			if (field.getKeyNode() instanceof ScalarNode key && key.getValue().equals("input"))
				return true;
		}
		return false;
	}

	private static Node caseNode(TestCase tc, boolean writesInput) {
		List<NodeTuple> fields = new ArrayList<>();
		fields.add(field("q", single(oneLine(tc.q))));
		if (writesInput || !tc.input.isNull())
			fields.add(field("input", json(tc.input)));
		fields.add(field("expectations", expectations(tc.expectations)));
		if (!tc.types.isEmpty())
			fields.add(field("types", types(tc.types)));
		if (!tc.properties.isEmpty())
			fields.add(field("properties", properties(tc.properties)));
		if (tc.comment != null)
			fields.add(field("comment", single(tc.comment)));
		if (tc.justification != null)
			fields.add(field("justification", plain(Tag.STR, tc.justification)));
		if (!tc.modules.isEmpty())
			fields.add(field("modules", modules(tc.modules)));
		if (tc.floatTolerance != null && tc.floatTolerance.ulps() != null)
			fields.add(field("float_tolerance", block(List.of(field("ulps", plain(Tag.INT, tc.floatTolerance.ulps().toString()))))));
		return block(fields);
	}

	private static Node expectations(TestCase.Expectations expectations) {
		List<NodeTuple> targets = new ArrayList<>();
		if (!expectations.defaultRows.isEmpty())
			targets.add(field("default", rows(expectations.defaultRows)));
		if (!expectations.overrides.isEmpty())
			targets.add(field("overrides", rows(expectations.overrides)));
		if (!expectations.jjq.isEmpty())
			targets.add(field("jjq", rows(expectations.jjq)));
		return block(targets);
	}

	private static Node rows(List<? extends TestCase.AbstractExpectation> expectations) {
		List<Node> rows = new ArrayList<>();
		for (TestCase.AbstractExpectation expectation : expectations) {
			List<NodeTuple> fields = new ArrayList<>();
			fields.add(field("v", version(expectation.version)));
			if (expectation instanceof TestCase.OverrideExpectation override) {
				if (override.os != null)
					fields.add(field("os", plain(Tag.STR, override.os.name())));
				if (override.arch != null)
					fields.add(field("arch", plain(Tag.STR, override.arch.name())));
			}
			if (expectation.output != null) {
				List<Node> values = new ArrayList<>();
				for (JsonNode value : expectation.output)
					values.add(json(value));
				fields.add(field("output", new SequenceNode(Tag.SEQ, values,
						values.isEmpty() ? DumperOptions.FlowStyle.FLOW : DumperOptions.FlowStyle.BLOCK)));
			}
			// A row says nothing about a flag it leaves out, so only a set one is written.
			if (expectation.runtimeError != null)
				fields.add(field("runtime_error", doubleQuoted(expectation.runtimeError)));
			if (expectation.compileError != null)
				fields.add(field("compile_error", doubleQuoted(expectation.compileError)));
			if (expectation.timedOut())
				fields.add(field("timeout", plain(Tag.BOOL, "true")));
			if (expectation.limitExceeded())
				fields.add(field("limit_exceeded", plain(Tag.BOOL, "true")));
			if (expectation.unstable())
				fields.add(field("unstable", plain(Tag.BOOL, "true")));
			if (expectation instanceof TestCase.JacksonJqExpectation jjq && jjq.incompatType != null)
				fields.add(field("incompat_type", plain(Tag.STR, jjq.incompatType.name())));
			rows.add(block(fields));
		}
		return new SequenceNode(Tag.SEQ, rows, DumperOptions.FlowStyle.BLOCK);
	}

	private static Node types(List<TestCase.TypeAssertion> types) {
		List<Node> rows = new ArrayList<>();
		for (TestCase.TypeAssertion type : types) {
			rows.add(block(List.of(
					field("v", version(type.version)),
					field("input", single(type.input)),
					field("output", single(type.output)))));
		}
		return new SequenceNode(Tag.SEQ, rows, DumperOptions.FlowStyle.BLOCK);
	}

	private static Node properties(List<TestCase.PropertyAssertion> properties) {
		List<Node> rows = new ArrayList<>();
		for (TestCase.PropertyAssertion property : properties) {
			rows.add(block(List.of(
					field("v", version(property.version)),
					field("cardinality", plain(Tag.STR, property.cardinality.name())),
					field("depends_on_input", plain(Tag.BOOL, Boolean.toString(property.dependsOnInput))),
					field("depends_on_external_state", plain(Tag.BOOL, Boolean.toString(property.dependsOnExternalState))))));
		}
		return new SequenceNode(Tag.SEQ, rows, DumperOptions.FlowStyle.BLOCK);
	}

	private static Node modules(Map<String, String> modules) {
		List<NodeTuple> fields = new ArrayList<>();
		modules.forEach((name, source) -> fields.add(new NodeTuple(
				plain(Tag.STR, name),
				new ScalarNode(Tag.STR, source.endsWith("\n") ? source : source + "\n", null, null, DumperOptions.ScalarStyle.LITERAL))));
		return block(fields);
	}

	/** A JSON value, as {@code format.py} wrote one: flow style, strings quoted, the rest plain. */
	private static Node json(JsonNode value) {
		if (value.isObject()) {
			List<NodeTuple> fields = new ArrayList<>();
			value.properties().forEach(entry -> fields.add(new NodeTuple(doubleQuoted(entry.getKey()), json(entry.getValue()))));
			return new MappingNode(Tag.MAP, fields, DumperOptions.FlowStyle.FLOW);
		}
		if (value.isArray()) {
			List<Node> values = new ArrayList<>();
			value.forEach(element -> values.add(json(element)));
			return new SequenceNode(Tag.SEQ, values, DumperOptions.FlowStyle.FLOW);
		}
		if (value.isTextual())
			return doubleQuoted(value.textValue());
		if (value.isNull())
			return plain(Tag.NULL, "null");
		if (value.isBoolean())
			return plain(Tag.BOOL, Boolean.toString(value.booleanValue()));
		if (value.isIntegralNumber())
			return plain(Tag.INT, value.bigIntegerValue().toString());
		if (value.isNumber()) {
			String literal = decimal(value.decimalValue());
			return plain(literal.contains(".") || literal.contains("e") ? Tag.FLOAT : Tag.INT, literal);
		}
		throw new IllegalArgumentException("not a JSON value: " + value.getNodeType());
	}

	/**
	 * A decimal as PyYAML wrote it, which is how every number in the corpus is spelled: a lowercase
	 * exponent that keeps its sign, and a mantissa that keeps its fraction.
	 */
	private static String decimal(BigDecimal value) {
		String text = value.toString().replace("E", "e");
		int exponent = text.indexOf('e');
		if (exponent < 0)
			return text;
		String mantissa = text.substring(0, exponent);
		String power = text.substring(exponent + 1);
		return (mantissa.contains(".") ? mantissa : mantissa + ".0")
				+ "e" + (power.startsWith("-") ? power : "+" + power.replace("+", ""));
	}

	/**
	 * A version range as the corpus spells it, e.g. {@code [1.7, 1.8.0)}.
	 *
	 * <p>{@link VersionRange#toString()} always writes three components per bound, while a test case
	 * names each release the way it was released. Tools that report a range to be compared against a
	 * file should spell it this way.
	 *
	 * @param range the range to spell
	 * @return the range as a test case writes it
	 */
	public static String range(VersionRange range) {
		StringBuilder text = new StringBuilder();
		text.append(range.minInclusive() ? '[' : '(');
		text.append(bound(range.minVersion()));
		text.append(", ");
		text.append(bound(range.maxVersion()));
		text.append(range.maxInclusive() ? ']' : ')');
		return text.toString();
	}

	private static Node version(VersionRange range) {
		return single(range(range));
	}

	private static String bound(@Nullable Version version) {
		return version == null ? "" : VersionSpelling.of(version);
	}

	/** Folds a multi-line query onto the single line the corpus keeps it on. */
	private static String oneLine(String q) {
		if (!q.contains("\n"))
			return q;
		List<String> parts = new ArrayList<>();
		for (String line : q.split("\n")) {
			if (!line.isBlank())
				parts.add(line.strip());
		}
		return String.join(" ", parts);
	}

	private static NodeTuple field(String name, Node value) {
		return new NodeTuple(plain(Tag.STR, name), value);
	}

	private static MappingNode block(List<NodeTuple> fields) {
		return new MappingNode(Tag.MAP, fields, DumperOptions.FlowStyle.BLOCK);
	}

	private static ScalarNode plain(Tag tag, String value) {
		return new ScalarNode(tag, value, null, null, DumperOptions.ScalarStyle.PLAIN);
	}

	private static ScalarNode single(String value) {
		return new ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.SINGLE_QUOTED);
	}

	private static ScalarNode doubleQuoted(String value) {
		return new ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
	}

	private TestCaseFormatter() {
	}
}
