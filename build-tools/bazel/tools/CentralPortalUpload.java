import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

/**
 * Signs a locally staged Maven repository and uploads it as one Central Portal bundle.
 *
 * <p>The staged tree is produced by {@code //:maven_install} with {@code MAVEN_REPO} pointing at a
 * local directory. Every file Central requires a signature for is detach-signed with the gpg key
 * already in the keyring, the whole tree is zipped into a single bundle, and the bundle is posted
 * to the Publisher API. The deployment is then polled until it reaches its terminal state.
 *
 * <p>Usage:
 *
 * <pre>
 *   CentralPortalUpload REPOSITORY
 *                       [--name NAME]
 *                       [--publishing-type USER_MANAGED|AUTOMATIC]
 *                       [--timeout SECONDS] [--interval SECONDS]
 *                       [--no-sign] [--gpg-key KEY]
 * </pre>
 *
 * <p>Credentials are read from the environment, never from the command line: {@code CENTRAL_TOKEN},
 * or {@code MAVEN_USER} and {@code MAVEN_PASSWORD} together. {@code GPG_PASSPHRASE} unlocks the
 * signing key. Run this outside Bazel -- {@code bazel run} would record the whole client
 * environment, credentials included, in {@code $(bazel info output_base)/java.log*}.
 */
public final class CentralPortalUpload {

	private static final String BASE_URL = "https://central.sonatype.com/api/v1/publisher";

	/**
	 * Central requires a signature for every deployed file, but not for checksums, and not for the
	 * signatures themselves. maven-metadata.xml is not part of a Portal bundle at all.
	 */
	private static final String SIGNATURE_SUFFIX = ".asc";

	private static final List<String> CHECKSUM_SUFFIXES = List.of(".md5", ".sha1", ".sha256", ".sha512");

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static final Option OPT_NAME = Option.builder()
			.longOpt("name")
			.desc("deployment name shown in the Portal (default: jackson-jq)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_PUBLISHING_TYPE = Option.builder()
			.longOpt("publishing-type")
			.desc("USER_MANAGED or AUTOMATIC (default: USER_MANAGED)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_TIMEOUT = Option.builder()
			.longOpt("timeout")
			.desc("seconds to wait for the deployment to settle (default: 1800)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_INTERVAL = Option.builder()
			.longOpt("interval")
			.desc("seconds between status polls (default: 10)")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_NO_SIGN = Option.builder()
			.longOpt("no-sign")
			.desc("the staged repository is already signed")
			.get();
	private static final Option OPT_GPG_KEY = Option.builder()
			.longOpt("gpg-key")
			.desc("key to sign with; gpg's default key otherwise")
			.numberOfArgs(1)
			.get();
	private static final Option OPT_HELP = Option.builder("h")
			.longOpt("help")
			.desc("print this message")
			.get();

	private CentralPortalUpload() {
	}

	/**
	 * A message meant for the operator rather than a stack trace: the counterpart of Python's
	 * {@code SystemExit}.
	 */
	static final class Failure extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Failure(String message) {
			super(message);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		try {
			run(args);
		} catch (Failure failure) {
			System.err.println(failure.getMessage());
			System.exit(1);
		}
	}

	private static void run(String[] args) throws IOException, InterruptedException {
		Options options = new Options();
		options.addOption(OPT_NAME);
		options.addOption(OPT_PUBLISHING_TYPE);
		options.addOption(OPT_TIMEOUT);
		options.addOption(OPT_INTERVAL);
		options.addOption(OPT_NO_SIGN);
		options.addOption(OPT_GPG_KEY);
		options.addOption(OPT_HELP);

		CommandLine command;
		try {
			command = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			throw new Failure("invalid arguments: " + e.getMessage());
		}
		if (command.hasOption(OPT_HELP.getLongOpt())) {
			HelpFormatter help = HelpFormatter.builder().get();
			help.printHelp("central_portal_upload [OPTIONS...] REPOSITORY", null, options, null, false);
			return;
		}

		List<String> rest = command.getArgList();
		if (rest.size() != 1)
			throw new Failure("Expected the root of one staged Maven repository, got " + rest.size() + " arguments.");
		Path repository = Paths.get(rest.get(0));
		if (!Files.isDirectory(repository))
			throw new Failure("Not a directory: " + repository);

		String name = command.getOptionValue(OPT_NAME.getLongOpt(), "jackson-jq");
		String publishingType = command.getOptionValue(OPT_PUBLISHING_TYPE.getLongOpt(), "USER_MANAGED");
		if (!"USER_MANAGED".equals(publishingType) && !"AUTOMATIC".equals(publishingType))
			throw new Failure("--publishing-type expects USER_MANAGED or AUTOMATIC: " + publishingType);
		int timeout = seconds(command, OPT_TIMEOUT, 1800);
		int interval = seconds(command, OPT_INTERVAL, 10);
		String gpgKey = command.getOptionValue(OPT_GPG_KEY.getLongOpt());

		// Resolved before signing: a missing credential should fail before the keyring is touched.
		String authorization = authorizationHeader();
		if (!command.hasOption(OPT_NO_SIGN.getLongOpt()))
			sign(repository, gpgKey, System.getenv("GPG_PASSPHRASE"));
		verifySigned(repository);

		Path directory = Files.createTempDirectory("jackson-jq-central-");
		String deploymentId;
		try {
			Path bundle = directory.resolve("central-bundle.zip");
			createBundle(repository, bundle);
			deploymentId = upload(bundle, name, publishingType, authorization);
		} finally {
			deleteRecursively(directory);
		}
		print("Uploaded Central deployment " + deploymentId);
		waitFor(deploymentId, "USER_MANAGED".equals(publishingType) ? "VALIDATED" : "PUBLISHED",
				authorization, timeout, interval);
	}

	private static int seconds(CommandLine command, Option option, int fallback) {
		String value = command.getOptionValue(option.getLongOpt());
		if (value == null)
			return fallback;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new Failure("--" + option.getLongOpt() + " expects a number of seconds: " + value);
		}
	}

	/**
	 * Every staged file that belongs in a Portal bundle, sorted so the bundle is deterministic.
	 */
	static List<Path> bundleFiles(Path repository) throws IOException {
		try (Stream<Path> tree = Files.walk(repository)) {
			return tree.filter(Files::isRegularFile)
					.filter(artifact -> !artifact.getFileName().toString().startsWith("maven-metadata"))
					.sorted()
					.collect(Collectors.toList());
		}
	}

	/**
	 * The staged files that Central requires a signature for.
	 */
	static List<Path> publishableFiles(Path repository) throws IOException {
		List<Path> publishable = new ArrayList<>();
		for (Path artifact : bundleFiles(repository)) {
			String name = artifact.getFileName().toString();
			if (name.endsWith(SIGNATURE_SUFFIX) || isChecksum(name))
				continue;
			publishable.add(artifact);
		}
		return publishable;
	}

	private static boolean isChecksum(String name) {
		for (String suffix : CHECKSUM_SUFFIXES)
			if (name.endsWith(suffix))
				return true;
		return false;
	}

	private static Path signatureOf(Path artifact) {
		return Paths.get(artifact + SIGNATURE_SUFFIX);
	}

	/**
	 * Detach-signs every publishable file with gpg, using the key already in the keyring.
	 */
	static void sign(Path repository, String key, String passphrase) throws IOException, InterruptedException {
		for (Path artifact : publishableFiles(repository)) {
			List<String> command = new ArrayList<>(List.of("gpg", "--batch", "--yes", "--armor", "--detach-sign"));
			if (key != null)
				command.addAll(List.of("--local-user", key));
			if (passphrase != null)
				// Never on argv: /proc/<pid>/cmdline is world-readable.
				command.addAll(List.of("--pinentry-mode", "loopback", "--passphrase-fd", "0"));
			command.addAll(List.of("--output", signatureOf(artifact).toString(), artifact.toString()));

			Process gpg = new ProcessBuilder(command)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.INHERIT)
					.start();
			try (OutputStream stdin = gpg.getOutputStream()) {
				stdin.write((passphrase == null ? "" : passphrase).getBytes(StandardCharsets.UTF_8));
			}
			if (gpg.waitFor() != 0)
				throw new Failure("gpg failed to sign " + artifact.getFileName());
		}
	}

	/**
	 * Refuses to build a bundle Central would reject for a missing signature.
	 */
	static void verifySigned(Path repository) throws IOException {
		List<String> missing = new ArrayList<>();
		for (Path artifact : publishableFiles(repository))
			if (!Files.exists(signatureOf(artifact)))
				missing.add(artifact.getFileName().toString());
		if (!missing.isEmpty())
			throw new Failure("No signature for " + missing.size() + " staged file(s):\n  "
					+ String.join("\n  ", missing));
	}

	static String authorizationHeader() {
		String token = System.getenv("CENTRAL_TOKEN");
		if (token != null && !token.isEmpty())
			return "Bearer " + token;

		String username = System.getenv("MAVEN_USER");
		String password = System.getenv("MAVEN_PASSWORD");
		if (username == null || username.isEmpty() || password == null || password.isEmpty())
			throw new Failure("Set CENTRAL_TOKEN to base64(username:password), "
					+ "or set both MAVEN_USER and MAVEN_PASSWORD.");
		byte[] credential = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
		return "Bearer " + Base64.getEncoder().encodeToString(credential);
	}

	static void createBundle(Path repository, Path output) throws IOException {
		try (ZipOutputStream bundle = new ZipOutputStream(Files.newOutputStream(output))) {
			for (Path artifact : bundleFiles(repository)) {
				bundle.putNextEntry(new ZipEntry(repository.relativize(artifact).toString()));
				Files.copy(artifact, bundle);
				bundle.closeEntry();
			}
		}
	}

	private static String upload(Path bundle, String name, String publishingType, String authorization)
			throws IOException, InterruptedException {
		String boundary = "----jackson-jq-" + UUID.randomUUID().toString().replace("-", "");
		byte[] preamble = ("--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"bundle\"; filename=\"" + bundle.getFileName() + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
		byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
		// Streamed rather than read whole: the bundle carries every artifact of a release.
		HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
				HttpRequest.BodyPublishers.ofByteArray(preamble),
				HttpRequest.BodyPublishers.ofFile(bundle),
				HttpRequest.BodyPublishers.ofByteArray(epilogue));
		String query = "name=" + encode(name) + "&publishingType=" + encode(publishingType);
		return request(BASE_URL + "/upload?" + query, authorization, body,
				"multipart/form-data; boundary=" + boundary).trim();
	}

	private static void waitFor(String deploymentId, String expected, String authorization, int timeout, int interval)
			throws IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout);
		while (System.nanoTime() < deadline) {
			JsonNode status = mapper.readTree(request(BASE_URL + "/status?id=" + encode(deploymentId),
					authorization, HttpRequest.BodyPublishers.noBody(), null));
			String state = status.path("deploymentState").asText();
			print("Central deployment " + deploymentId + ": " + state);
			if (expected.equals(state))
				return;
			if ("FAILED".equals(state))
				throw new Failure(mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(status.has("errors") ? status.get("errors") : status));
			TimeUnit.SECONDS.sleep(interval);
		}
		throw new Failure("Timed out waiting for Central deployment " + deploymentId);
	}

	private static String request(String url, String authorization, HttpRequest.BodyPublisher body, String contentType)
			throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
				.header("Authorization", authorization)
				.header("Accept", "application/json")
				// Every Publisher API endpoint this tool calls is a POST, status included. An empty
				// body rather than none, so that Content-Length: 0 is still sent.
				.POST(body);
		if (contentType != null)
			request.header("Content-Type", contentType);
		HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 300)
			throw new Failure("Central Portal returned HTTP " + response.statusCode() + ": " + response.body());
		return response.body();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Progress goes out unbuffered: this runs under CI log streaming.
	 */
	private static void print(String message) {
		System.out.println(message);
		System.out.flush();
	}

	private static void deleteRecursively(Path directory) throws IOException {
		try (Stream<Path> tree = Files.walk(directory)) {
			for (Path path : tree.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))
				Files.deleteIfExists(path);
		}
	}
}
