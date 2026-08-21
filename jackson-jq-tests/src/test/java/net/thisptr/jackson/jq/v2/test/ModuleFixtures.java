package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Materializes {@link TestCase#modules} maps into temp directories suitable as a jq module
 * search root, for both real jq ({@code -L}) and this library's own {@code FileSystemModuleLoader}.
 */
public final class ModuleFixtures {

	public static Path materialize(Map<String, String> modules) throws IOException {
		Path dir = Files.createTempDirectory("jq-modules");
		for (Map.Entry<String, String> entry : modules.entrySet()) {
			Path file = dir.resolve(entry.getKey());
			Files.createDirectories(file.getParent());
			Files.write(file, entry.getValue().getBytes(StandardCharsets.UTF_8));
		}
		return dir;
	}

	public static void cleanup(Path dir) throws IOException {
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private ModuleFixtures() {}
}
