import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Merges package jars and assembles multi-release jars.
 *
 * <p>Usage:
 *
 * <pre>
 *   MultiReleaseJar --output out.jar
 *                   [--base base.jar]
 *                   [--input package1.jar] [--input package2.jar] ...
 *                   [--overlay 9=module-info.class]
 *                   [--overlay 17=java17-classes.jar]
 *                   [--multi-release]
 *                   [--no-manifest]
 * </pre>
 *
 * <p>All input jars are merged into the root namespace. For {@code META-INF/services/*},
 * service provider lines are merged in order with duplicates removed. Duplicate class or
 * resource files with identical content are deduplicated.
 *
 * <p>An overlay whose file is a jar contributes all of its entries; any other file is
 * placed at {@code META-INF/versions/N/<file name>}. Output is deterministic: entries are
 * emitted in sorted order with a fixed timestamp.
 */
public final class MultiReleaseJar {

	/**
	 * DOS epoch, as used by every reproducible-jar tool.
	 */
	private static final long FIXED_TIME = 315532800000L;

	private static final String SERVICES_PREFIX = "META-INF/services/";

	private MultiReleaseJar() {
	}

	public static void main(String[] args) throws IOException {
		Path base = null;
		Path output = null;
		List<Path> inputs = new ArrayList<>();
		List<String> overlays = new ArrayList<>();
		boolean multiRelease = false;
		boolean noManifest = false;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
				case "--base":
					base = Paths.get(args[++i]);
					break;
				case "--input":
					inputs.add(Paths.get(args[++i]));
					break;
				case "--output":
					output = Paths.get(args[++i]);
					break;
				case "--overlay":
					overlays.add(args[++i]);
					break;
				case "--multi-release":
					multiRelease = true;
					break;
				case "--no-manifest":
					noManifest = true;
					break;
				default:
					throw new IllegalArgumentException("unknown option: " + args[i]);
			}
		}
		if (output == null)
			throw new IllegalArgumentException("--output is required");
		if (base == null && inputs.isEmpty())
			throw new IllegalArgumentException("At least one --base or --input is required");

		Map<String, byte[]> entries = new TreeMap<>();
		Map<String, Set<String>> services = new LinkedHashMap<>();
		Manifest manifest = null;

		if (base != null) {
			try (JarFile jar = new JarFile(base.toFile())) {
				if (!noManifest)
					manifest = jar.getManifest();
				readInto(jar, "", entries, services);
			}
		}

		for (Path input : inputs) {
			try (JarFile jar = new JarFile(input.toFile())) {
				if (manifest == null && !noManifest)
					manifest = jar.getManifest();
				readInto(jar, "", entries, services);
			}
		}

		for (String overlay : overlays) {
			int eq = overlay.indexOf('=');
			if (eq < 0)
				throw new IllegalArgumentException("--overlay expects VERSION=PATH: " + overlay);
			String version = overlay.substring(0, eq);
			Path path = Paths.get(overlay.substring(eq + 1));
			String prefix = "META-INF/versions/" + version + "/";
			if (path.getFileName().toString().endsWith(".jar")) {
				try (JarFile jar = new JarFile(path.toFile())) {
					readInto(jar, prefix, entries, services);
				}
			} else {
				entries.put(prefix + path.getFileName(), Files.readAllBytes(path));
			}
		}

		// Write merged services into entries
		for (Map.Entry<String, Set<String>> service : services.entrySet()) {
			StringBuilder sb = new StringBuilder();
			for (String line : service.getValue()) {
				sb.append(line).append('\n');
			}
			entries.put(service.getKey(), sb.toString().getBytes(StandardCharsets.UTF_8));
		}

		if (multiRelease) {
			if (manifest == null)
				manifest = new Manifest();
			manifest.getMainAttributes().putValue("Multi-Release", "true");
		}
		if (manifest != null && manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION) == null) {
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		}

		Files.createDirectories(output.toAbsolutePath().getParent());
		try (OutputStream out = Files.newOutputStream(output);
			 JarOutputStream jar = new JarOutputStream(out)) {
			if (!noManifest && manifest != null) {
				JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
				manifestEntry.setTime(FIXED_TIME);
				jar.putNextEntry(manifestEntry);
				manifest.write(jar);
				jar.closeEntry();
			}

			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				JarEntry jarEntry = new JarEntry(entry.getKey());
				jarEntry.setTime(FIXED_TIME);
				jar.putNextEntry(jarEntry);
				jar.write(entry.getValue());
				jar.closeEntry();
			}
		}
	}

	private static void readInto(JarFile jar, String prefix, Map<String, byte[]> entries, Map<String, Set<String>> services) throws IOException {
		for (ZipEntry entry : java.util.Collections.list(jar.entries())) {
			if (entry.isDirectory())
				continue;
			String name = entry.getName();
			// The manifest is rebuilt; signatures would be invalidated by rewriting.
			if (JarFile.MANIFEST_NAME.equals(name))
				continue;

			if (prefix.isEmpty() && name.startsWith(SERVICES_PREFIX) && !name.equals(SERVICES_PREFIX)) {
				Set<String> lines = services.computeIfAbsent(name, k -> new LinkedHashSet<>());
				try (InputStream in = jar.getInputStream(entry);
					 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String trimmed = line.trim();
						if (!trimmed.isEmpty()) {
							lines.add(trimmed);
						}
					}
				}
				continue;
			}

			byte[] content;
			try (InputStream in = jar.getInputStream(entry)) {
				content = readAll(in);
			}

			String destName = prefix + name;
			byte[] existing = entries.get(destName);
			if (existing != null) {
				if (!Arrays.equals(existing, content)) {
					throw new IllegalStateException("Duplicate entry with different contents: " + destName);
				}
			} else {
				entries.put(destName, content);
			}
		}
	}

	private static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int n;
		while ((n = in.read(chunk)) > 0)
			buffer.write(chunk, 0, n);
		return buffer.toByteArray();
	}
}
