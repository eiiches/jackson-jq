package net.thisptr.jackson.jq.v2.ext.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.thisptr.jackson.jq.v2.core.Environment;
import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.JsonQuery;
import net.thisptr.jackson.jq.v2.core.RuntimeOptions;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.gson.GsonJsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FsModuleTest {
	private static final String IMPORT = "import \"jackson-jq/fs\" as fs; ";
	private static final JsonProvider<JsonNode> JSON_PROVIDER = Jackson2JsonProvider.getInstance();

	@TempDir
	Path directory;

	@Test
	public void exposesFunctions() {
		assertThat(new ModuleImpl().getFunctions().keySet()).containsExactlyInAnyOrder(
				FunctionSignature.of("read_text", 1),
				FunctionSignature.of("read_text", 2),
				FunctionSignature.of("read_binary", 1),
				FunctionSignature.of("write_text", 1),
				FunctionSignature.of("write_text", 2),
				FunctionSignature.of("write_binary", 1),
				FunctionSignature.of("write_binary", 2),
				FunctionSignature.of("list", 1),
				FunctionSignature.of("list", 2),
				FunctionSignature.of("read_json", 1),
				FunctionSignature.of("read_json", 2),
				FunctionSignature.of("read_json_stream", 1),
				FunctionSignature.of("read_json_stream", 2),
				FunctionSignature.of("write_json", 1),
				FunctionSignature.of("write_json", 2));
	}

	@Test
	public void readsTextWithDefaultAndSelectedEncoding() throws IOException {
		Path utf8 = directory.resolve("utf8.txt");
		Files.write(utf8, "こんにちは".getBytes(StandardCharsets.UTF_8));
		assertThat(run("fs::read_text(" + quote(utf8) + ")", unlimited()).get(0).textValue()).isEqualTo("こんにちは");

		Path latin1 = directory.resolve("latin1.txt");
		Files.write(latin1, "café".getBytes(StandardCharsets.ISO_8859_1));
		assertThat(run("fs::read_text(" + quote(latin1) + "; {encoding: \"ISO-8859-1\"})", unlimited()).get(0).textValue()).isEqualTo("café");
		assertThat(run("fs::read_text(" + quote(utf8) + "; {})", unlimited()).get(0).textValue()).isEqualTo("こんにちは");

		Path empty = directory.resolve("empty.txt");
		Files.write(empty, new byte[0]);
		assertThat(run("fs::read_text(" + quote(empty) + ")", unlimited()).get(0).textValue()).isEmpty();
	}

	@Test
	public void rejectsMalformedTextAndInvalidReadArguments() throws IOException {
		Path malformed = directory.resolve("malformed.txt");
		Files.write(malformed, new byte[] { (byte) 0xc3, 0x28 });
		assertThatThrownBy(() -> run("fs::read_text(" + quote(malformed) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for")
				.hasMessageContaining("UTF-8");
		assertThatThrownBy(() -> run("fs::read_text(null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::read_text(" + quote(malformed) + "; null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::read_text(" + quote(malformed) + "; {extra: true})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::read_text(" + quote(malformed) + "; {encoding: 1})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("encoding must be a string");
		assertThatThrownBy(() -> run("fs::read_text(" + quote(malformed) + "; {encoding: \"not-a-charset\"})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unsupported encoding");
	}

	@Test
	public void readsBinaryAndFallsBackToBase64() throws IOException {
		Path file = directory.resolve("binary.dat");
		byte[] bytes = { 0, 1, (byte) 0xff };
		Files.write(file, bytes);
		assertThat(run("fs::read_binary(" + quote(file) + ")", unlimited()).get(0).binaryValue()).containsExactly(bytes);

		JsonProvider<JsonElement> provider = GsonJsonProvider.getInstance();
		JsonElement result = run(provider, "fs::read_binary(" + quote(provider, file) + ")", unlimited()).get(0);
		assertThat(provider.getString(result)).isEqualTo(Base64.getEncoder().encodeToString(bytes));
	}

	@Test
	public void writesTextWithDefaultAndSelectedEncodingAndReturnsNull() throws IOException {
		Path file = directory.resolve("text.txt");
		JsonNode result = run("fs::write_text(" + quote(file) + ")", JSON_PROVIDER.createString("こんにちは"), unlimited()).get(0);
		assertThat(result.isNull()).isTrue();
		assertThat(Files.readAllBytes(file)).isEqualTo("こんにちは".getBytes(StandardCharsets.UTF_8));

		Files.write(file, "a longer previous value".getBytes(StandardCharsets.UTF_8));
		run("fs::write_text(" + quote(file) + "; {encoding: \"ISO-8859-1\"})", JSON_PROVIDER.createString("café"), unlimited());
		assertThat(Files.readAllBytes(file)).isEqualTo("café".getBytes(StandardCharsets.ISO_8859_1));

		run("fs::write_text(" + quote(file) + ")", JSON_PROVIDER.createString(""), unlimited());
		assertThat(Files.readAllBytes(file)).isEmpty();

		Path appendTextFile = directory.resolve("append_text.txt");
		run("fs::write_text(" + quote(appendTextFile) + "; {append: true})", JSON_PROVIDER.createString("hello "), unlimited());
		run("fs::write_text(" + quote(appendTextFile) + "; {append: true})", JSON_PROVIDER.createString("world"), unlimited());
		assertThat(new String(Files.readAllBytes(appendTextFile), StandardCharsets.UTF_8)).isEqualTo("hello world");
		run("fs::write_text(" + quote(appendTextFile) + "; {append: false})", JSON_PROVIDER.createString("replaced"), unlimited());
		assertThat(new String(Files.readAllBytes(appendTextFile), StandardCharsets.UTF_8)).isEqualTo("replaced");
	}

	@Test
	public void writesNativeBinaryAndBase64String() throws IOException {
		Path nativeFile = directory.resolve("native.dat");
		byte[] bytes = { 0, 1, (byte) 0xff };
		run("fs::write_binary(" + quote(nativeFile) + ")", JSON_PROVIDER.createBinary(bytes), unlimited());
		assertThat(Files.readAllBytes(nativeFile)).containsExactly(bytes);

		Path base64File = directory.resolve("base64.dat");
		JsonProvider<JsonElement> provider = GsonJsonProvider.getInstance();
		JsonElement input = provider.createString(Base64.getEncoder().encodeToString(bytes));
		JsonElement result = run(provider, "fs::write_binary(" + quote(provider, base64File) + ")", input, unlimited()).get(0);
		assertThat(provider.isNull(result)).isTrue();
		assertThat(Files.readAllBytes(base64File)).containsExactly(bytes);

		Path appendBinaryFile = directory.resolve("append_binary.dat");
		run("fs::write_binary(" + quote(appendBinaryFile) + "; {append: true})", JSON_PROVIDER.createBinary(new byte[] { 1, 2 }), unlimited());
		run("fs::write_binary(" + quote(appendBinaryFile) + "; {append: true})", JSON_PROVIDER.createBinary(new byte[] { 3, 4 }), unlimited());
		assertThat(Files.readAllBytes(appendBinaryFile)).containsExactly(1, 2, 3, 4);
		run("fs::write_binary(" + quote(appendBinaryFile) + "; {append: false})", JSON_PROVIDER.createBinary(new byte[] { 9 }), unlimited());
		assertThat(Files.readAllBytes(appendBinaryFile)).containsExactly(9);
	}

	@Test
	public void rejectsInvalidWriteArgumentsWithoutChangingExistingFile() throws IOException {
		Path file = directory.resolve("existing.txt");
		byte[] original = "original".getBytes(StandardCharsets.UTF_8);
		Files.write(file, original);

		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + ")", JSON_PROVIDER.createNull(), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("requires string input");
		assertThatThrownBy(() -> run("fs::write_text(null)", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; null)", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {extra: true})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {encoding: 1})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("encoding must be a string");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {encoding: \"not-a-charset\"})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unsupported encoding");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {encoding: \"US-ASCII\"})", JSON_PROVIDER.createString("é"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed to encode");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {append: 1})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("append must be a boolean");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {create_parents: 1})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("create_parents must be a boolean");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(file) + "; {mkdirs: 1})", JSON_PROVIDER.createString("replacement"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("mkdirs must be a boolean");
		assertThat(Files.readAllBytes(file)).containsExactly(original);

		Path binary = directory.resolve("binary.dat");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + ")", JSON_PROVIDER.createString("not base64"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("valid Base64");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + ")", JSON_PROVIDER.createNumber(1), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("requires binary or Base64 string input");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; null)", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; {unknown: true})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; {encoding: \"UTF-8\"})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; {append: 1})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("append must be a boolean");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; {create_parents: 1})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("create_parents must be a boolean");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(binary) + "; {mkdirs: 1})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("mkdirs must be a boolean");
		assertThat(binary).doesNotExist();

		Path missingParent = directory.resolve("missing").resolve("file.txt");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(missingParent) + ")", JSON_PROVIDER.createString("value"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
		assertThatThrownBy(() -> run("fs::write_text(" + quote(missingParent) + "; {create_parents: false})", JSON_PROVIDER.createString("value"), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
		Path missingBinaryParent = directory.resolve("missing").resolve("file.dat");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(missingBinaryParent) + ")", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
		assertThatThrownBy(() -> run("fs::write_binary(" + quote(missingBinaryParent) + "; {mkdirs: false})", JSON_PROVIDER.createBinary(new byte[] { 1 }), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
	}

	@Test
	public void listsImmediateEntriesAsSortedMetadata() throws IOException {
		Files.write(directory.resolve("z.txt"), new byte[] { 1 });
		Files.createDirectory(directory.resolve("a-dir"));

		JsonNode result = run("fs::list(" + quote(directory) + ")", unlimited()).get(0);
		assertThat(result).hasSize(2);
		assertEntry(result.get(0), "a-dir", "directory");
		assertEntry(result.get(1), "z.txt", "file");
	}

	@Test
	public void recursivelyListsDescendantsAndCanAvoidFollowingSymlinks() throws IOException {
		Path target = Files.createDirectory(directory.resolve("target"));
		Files.write(target.resolve("value.txt"), new byte[] { 1 });
		Path alias = directory.resolve("alias");
		Files.createSymbolicLink(alias, target);

		JsonNode followed = run("fs::list(" + quote(directory) + "; {recursive: true})", unlimited()).get(0);
		assertThat(paths(followed)).containsExactly("alias", path("alias", "value.txt"), "target", path("target", "value.txt"));
		assertEntry(followed.get(0), "alias", "symlink");

		JsonNode notFollowed = run("fs::list(" + quote(directory) + "; {recursive: true, follow_symlinks: false})", unlimited()).get(0);
		assertThat(paths(notFollowed)).containsExactly("alias", "target", path("target", "value.txt"));
	}

	@Test
	public void recursiveListingSkipsAncestorSymlinkCycles() throws IOException {
		Path child = Files.createDirectory(directory.resolve("child"));
		Files.write(child.resolve("value.txt"), new byte[] { 1 });
		Files.createSymbolicLink(child.resolve("back"), directory);

		JsonNode result = run("fs::list(" + quote(directory) + "; {recursive: true})", unlimited()).get(0);
		assertThat(paths(result)).containsExactly("child", path("child", "back"), path("child", "value.txt"));
		assertThat(result.get(1).get("type").textValue()).isEqualTo("symlink");
	}

	@Test
	public void rejectsInvalidListArguments() throws IOException {
		Path file = directory.resolve("file");
		Files.write(file, new byte[0]);
		assertThatThrownBy(() -> run("fs::list(null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::list(" + quote(file) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("not a directory");
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + "; [])", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + "; {unknown: true})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + "; {recursive: 1})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("recursive must be a boolean");
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + "; {follow_symlinks: 1})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("follow_symlinks must be a boolean");
	}

	@Test
	public void enforcesRuntimeLimits() throws IOException {
		Path file = directory.resolve("value.txt");
		Files.write(file, "hello".getBytes(StandardCharsets.UTF_8));
		RuntimeOptions shortString = RuntimeOptions.newBuilder().setMaxStringLength(4).build();
		assertThatThrownBy(() -> run("fs::read_text(" + quote(file) + ")", shortString))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum string length of 4");

		RuntimeOptions shortBinary = RuntimeOptions.newBuilder().setMaxBinaryLength(4).build();
		assertThatThrownBy(() -> run("fs::read_binary(" + quote(file) + ")", shortBinary))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum binary length of 4");

		Files.write(directory.resolve("second.txt"), new byte[0]);
		RuntimeOptions shortArray = RuntimeOptions.newBuilder().setMaxArrayLength(1).build();
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + ")", shortArray))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum array length of 1");

		RuntimeOptions shortObject = RuntimeOptions.newBuilder().setMaxObjectMemberCount(1).build();
		assertThatThrownBy(() -> run("fs::list(" + quote(directory) + ")", shortObject))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("maximum object member count of 1");
	}

	@Test
	public void readsJsonValue() throws IOException {
		Path file = directory.resolve("data.json");
		Files.write(file, "{\"name\": \"alice\", \"age\": 30, \"active\": true, \"tags\": [1, null]}".getBytes(StandardCharsets.UTF_8));
		JsonNode result = run("fs::read_json(" + quote(file) + ")", unlimited()).get(0);
		assertThat(result.get("name").textValue()).isEqualTo("alice");
		assertThat(result.get("age").intValue()).isEqualTo(30);
		assertThat(result.get("active").booleanValue()).isTrue();
		assertThat(result.get("tags").get(0).intValue()).isEqualTo(1);
		assertThat(result.get("tags").get(1).isNull()).isTrue();

		JsonNode withEmptyOptions = run("fs::read_json(" + quote(file) + "; {})", unlimited()).get(0);
		assertThat(withEmptyOptions.get("name").textValue()).isEqualTo("alice");

		Path primitive = directory.resolve("num.json");
		Files.write(primitive, "123".getBytes(StandardCharsets.UTF_8));
		assertThat(run("fs::read_json(" + quote(primitive) + ")", unlimited()).get(0).intValue()).isEqualTo(123);
	}

	@Test
	public void rejectsInvalidReadJsonArgumentsAndFiles() throws IOException {
		Path empty = directory.resolve("empty.json");
		Files.write(empty, new byte[0]);
		assertThatThrownBy(() -> run("fs::read_json(" + quote(empty) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("empty input");

		Path trailing = directory.resolve("trailing.json");
		Files.write(trailing, "{\"a\": 1} {\"b\": 2}".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> run("fs::read_json(" + quote(trailing) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("trailing content");

		Path malformed = directory.resolve("malformed.json");
		Files.write(malformed, "{\"a\":".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> run("fs::read_json(" + quote(malformed) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");

		assertThatThrownBy(() -> run("fs::read_json(null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::read_json(" + quote(empty) + "; null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::read_json(" + quote(empty) + "; {unknown: true})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options contains unknown member");
	}

	@Test
	public void readsJsonStream() throws IOException {
		Path streamFile = directory.resolve("stream.jsonl");
		Files.write(streamFile, """
				{"id": 1, "name": "a"}
				{"id": 2, "name": "b"}
				""".getBytes(StandardCharsets.UTF_8));

		List<JsonNode> results = run("fs::read_json_stream(" + quote(streamFile) + ")", unlimited());
		assertThat(results).hasSize(2);
		assertThat(results.get(0).get("id").intValue()).isEqualTo(1);
		assertThat(results.get(1).get("id").intValue()).isEqualTo(2);

		List<JsonNode> projected = run("fs::read_json_stream(" + quote(streamFile) + ") | .name", unlimited());
		assertThat(projected).extracting(JsonNode::textValue).containsExactly("a", "b");

		List<JsonNode> withOptions = run("fs::read_json_stream(" + quote(streamFile) + "; {})", unlimited());
		assertThat(withOptions).hasSize(2);

		Path empty = directory.resolve("empty_stream.json");
		Files.write(empty, new byte[0]);
		assertThat(run("fs::read_json_stream(" + quote(empty) + ")", unlimited())).isEmpty();

		Path whitespaceOnly = directory.resolve("whitespace.json");
		Files.write(whitespaceOnly, "   \n\t  ".getBytes(StandardCharsets.UTF_8));
		assertThat(run("fs::read_json_stream(" + quote(whitespaceOnly) + ")", unlimited())).isEmpty();

		Path spaceSeparated = directory.resolve("values.json");
		Files.write(spaceSeparated, "1 \"two\" true [3] {\"k\": 4}".getBytes(StandardCharsets.UTF_8));
		List<JsonNode> values = run("fs::read_json_stream(" + quote(spaceSeparated) + ")", unlimited());
		assertThat(values).hasSize(5);
		assertThat(values.get(0).intValue()).isEqualTo(1);
		assertThat(values.get(1).textValue()).isEqualTo("two");
		assertThat(values.get(2).booleanValue()).isTrue();
		assertThat(values.get(3).get(0).intValue()).isEqualTo(3);
		assertThat(values.get(4).get("k").intValue()).isEqualTo(4);

		Path malformedStream = directory.resolve("bad_stream.json");
		Files.write(malformedStream, "{\"id\": 1} invalid".getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> run("fs::read_json_stream(" + quote(malformedStream) + ")", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");

		assertThatThrownBy(() -> run("fs::read_json_stream(null)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::read_json_stream(" + quote(streamFile) + "; 123)", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::read_json_stream(" + quote(streamFile) + "; {unknown: 1})", unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
	}

	@Test
	public void writesJsonWithDefaultAndSelectedOptionsAndReturnsNull() throws IOException {
		Path file = directory.resolve("output.json");
		JsonNode input = run("{\"b\": 2, \"a\": [1, 2]}", unlimited()).get(0);

		JsonNode result = run("fs::write_json(" + quote(file) + ")", input, unlimited()).get(0);
		assertThat(result.isNull()).isTrue();
		assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("{\"b\":2,\"a\":[1,2]}\n");

		run("fs::write_json(" + quote(file) + "; {newline: false})", input, unlimited());
		assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("{\"b\":2,\"a\":[1,2]}");

		run("fs::write_json(" + quote(file) + "; {indent: 2})", input, unlimited());
		String indented = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertThat(indented).isEqualTo("{\n  \"b\": 2,\n  \"a\": [\n    1,\n    2\n  ]\n}\n");

		run("fs::write_json(" + quote(file) + "; {indent: \"\\t\"})", input, unlimited());
		String tabIndented = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertThat(tabIndented).isEqualTo("{\n\t\"b\": 2,\n\t\"a\": [\n\t\t1,\n\t\t2\n\t]\n}\n");

		run("fs::write_json(" + quote(file) + "; {indent: true})", input, unlimited());
		assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo(indented);

		run("fs::write_json(" + quote(file) + "; {indent: false})", input, unlimited());
		assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("{\"b\":2,\"a\":[1,2]}\n");

		Path appendFile = directory.resolve("append.jsonl");
		run("fs::write_json(" + quote(appendFile) + "; {append: true})", run("{\"line\": 1}", unlimited()).get(0), unlimited());
		run("fs::write_json(" + quote(appendFile) + "; {append: true})", run("{\"line\": 2}", unlimited()).get(0), unlimited());
		assertThat(new String(Files.readAllBytes(appendFile), StandardCharsets.UTF_8)).isEqualTo("{\"line\":1}\n{\"line\":2}\n");

		Path encodedFile = directory.resolve("encoded.json");
		run("fs::write_json(" + quote(encodedFile) + "; {encoding: \"ISO-8859-1\"})", run("{\"msg\": \"café\"}", unlimited()).get(0), unlimited());
		assertThat(new String(Files.readAllBytes(encodedFile), StandardCharsets.ISO_8859_1)).isEqualTo("{\"msg\":\"café\"}\n");

		JsonProvider<JsonElement> provider = GsonJsonProvider.getInstance();
		JsonElement gsonInput = run(provider, "{\"x\": 1}", unlimited()).get(0);
		Path gsonFile = directory.resolve("gson.json");
		JsonElement gsonResult = run(provider, "fs::write_json(" + quote(provider, gsonFile) + ")", gsonInput, unlimited()).get(0);
		assertThat(provider.isNull(gsonResult)).isTrue();
		assertThat(new String(Files.readAllBytes(gsonFile), StandardCharsets.UTF_8)).isEqualTo("{\"x\":1}\n");
	}

	@Test
	public void rejectsInvalidWriteJsonArgumentsWithoutChangingExistingFile() throws IOException {
		Path file = directory.resolve("existing.json");
		byte[] original = "{\"keep\": true}\n".getBytes(StandardCharsets.UTF_8);
		Files.write(file, original);

		JsonNode input = run("{\"a\": 1}", unlimited()).get(0);

		assertThatThrownBy(() -> run("fs::write_json(null)", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("path must be a string");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; null)", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("options must be an object");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {unknown: true})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unknown member");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {indent: -1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("indent must be a non-negative integer");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {indent: []})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("indent must be a non-negative integer");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {append: 1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("append must be a boolean");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {newline: 1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("newline must be a boolean");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {encoding: 1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("encoding must be a string");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {encoding: \"not-a-charset\"})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("unsupported encoding");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {encoding: \"US-ASCII\"})", run("{\"msg\": \"é\"}", unlimited()).get(0), unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed to encode");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {create_parents: 1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("create_parents must be a boolean");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(file) + "; {mkdirs: 1})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("mkdirs must be a boolean");

		assertThat(Files.readAllBytes(file)).isEqualTo(original);

		Path missingParent = directory.resolve("missing").resolve("out.json");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(missingParent) + ")", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(missingParent) + "; {create_parents: false})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
		assertThatThrownBy(() -> run("fs::write_json(" + quote(missingParent) + "; {mkdirs: false})", input, unlimited()))
				.isInstanceOf(JsonQueryException.class)
				.hasMessageContaining("failed for");
	}

	@Test
	public void createsParentDirectoriesWhenRequested() throws IOException {
		Path textFile1 = directory.resolve("nested1").resolve("sub1").resolve("hello.txt");
		run("fs::write_text(" + quote(textFile1) + "; {create_parents: true})", JSON_PROVIDER.createString("hello"), unlimited());
		assertThat(Files.readAllBytes(textFile1)).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));

		Path textFile2 = directory.resolve("nested2").resolve("sub2").resolve("hello.txt");
		run("fs::write_text(" + quote(textFile2) + "; {mkdirs: true})", JSON_PROVIDER.createString("world"), unlimited());
		assertThat(Files.readAllBytes(textFile2)).isEqualTo("world".getBytes(StandardCharsets.UTF_8));

		Path binaryFile1 = directory.resolve("nested3").resolve("sub3").resolve("data.bin");
		byte[] bytes1 = { 1, 2, 3 };
		run("fs::write_binary(" + quote(binaryFile1) + "; {create_parents: true})", JSON_PROVIDER.createBinary(bytes1), unlimited());
		assertThat(Files.readAllBytes(binaryFile1)).containsExactly(bytes1);

		Path binaryFile2 = directory.resolve("nested4").resolve("sub4").resolve("data.bin");
		byte[] bytes2 = { 4, 5, 6 };
		run("fs::write_binary(" + quote(binaryFile2) + "; {mkdirs: true})", JSON_PROVIDER.createBinary(bytes2), unlimited());
		assertThat(Files.readAllBytes(binaryFile2)).containsExactly(bytes2);

		Path jsonFile1 = directory.resolve("nested5").resolve("sub5").resolve("data.json");
		run("fs::write_json(" + quote(jsonFile1) + "; {create_parents: true})", run("{\"a\": 1}", unlimited()).get(0), unlimited());
		assertThat(new String(Files.readAllBytes(jsonFile1), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}\n");

		Path jsonFile2 = directory.resolve("nested6").resolve("sub6").resolve("data.json");
		run("fs::write_json(" + quote(jsonFile2) + "; {mkdirs: true})", run("{\"b\": 2}", unlimited()).get(0), unlimited());
		assertThat(new String(Files.readAllBytes(jsonFile2), StandardCharsets.UTF_8)).isEqualTo("{\"b\":2}\n");

		Path combinedFile = directory.resolve("nested7").resolve("sub7").resolve("stream.jsonl");
		run("fs::write_json(" + quote(combinedFile) + "; {create_parents: true, append: true})", run("{\"line\": 1}", unlimited()).get(0), unlimited());
		run("fs::write_json(" + quote(combinedFile) + "; {create_parents: true, append: true})", run("{\"line\": 2}", unlimited()).get(0), unlimited());
		assertThat(new String(Files.readAllBytes(combinedFile), StandardCharsets.UTF_8)).isEqualTo("{\"line\":1}\n{\"line\":2}\n");
	}

	private static void assertEntry(JsonNode node, String path, String type) {
		assertThat(node.get("path").textValue()).isEqualTo(path);
		assertThat(node.get("type").textValue()).isEqualTo(type);
	}

	private static List<String> paths(JsonNode array) {
		java.util.ArrayList<String> result = new java.util.ArrayList<>();
		array.forEach(entry -> result.add(entry.get("path").textValue()));
		return result;
	}

	private static String path(String first, String second) {
		return java.nio.file.Paths.get(first, second).toString();
	}

	private static String quote(Path path) {
		return quote(JSON_PROVIDER, path);
	}

	private static <JsonNode> String quote(JsonProvider<JsonNode> provider, Path path) {
		return provider.format(provider.createString(path.toString()));
	}

	private static RuntimeOptions unlimited() {
		return RuntimeOptions.newBuilder().build();
	}

	private static List<JsonNode> run(String expression, RuntimeOptions options) {
		return run(JSON_PROVIDER, expression, options);
	}

	private static List<JsonNode> run(String expression, JsonNode input, RuntimeOptions options) {
		return run(JSON_PROVIDER, expression, input, options);
	}

	private static <JsonNode> List<JsonNode> run(JsonProvider<JsonNode> provider, String expression, RuntimeOptions options) {
		return run(provider, expression, provider.createNull(), options);
	}

	private static <JsonNode> List<JsonNode> run(JsonProvider<JsonNode> provider, String expression, JsonNode input, RuntimeOptions options) {
		Environment<JsonNode> environment = EnvironmentBuilder.withDefaultLoaders(provider, Versions.JQ_1_8_2).build();
		JsonQuery<JsonNode> query = environment.compile(IMPORT + expression).withRuntimeOptions(options);
		return query.apply(input);
	}
}
