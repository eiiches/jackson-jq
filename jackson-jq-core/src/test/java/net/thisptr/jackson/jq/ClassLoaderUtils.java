package net.thisptr.jackson.jq;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderUtils {
	private static final Logger log = LoggerFactory.getLogger(ClassLoaderUtils.class);

	public static final FileSystem fileSystem = createFileSystem();

	private static FileSystem createFileSystem() {
		// This hack registers NativeImageResourceFileSystem when ran via Native Image
		// https://github.com/oracle/graal/issues/7682
		try {
			FileSystem fileSystem = FileSystems.newFileSystem(
				URI.create("resource:/"),
				Collections.singletonMap("create", "true")
			);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					fileSystem.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
			return fileSystem;
		} catch (Exception e) {
			log.info("Not running in native image, skipping");
		}
		return FileSystems.getDefault();
	}

	public static Path resolve(String fileName) {
		URL url = ClassLoaderUtils.class.getClassLoader().getResource(fileName);
		return fileSystem.getPath(Objects.requireNonNull(url).getPath());
	}

	public static void walk(String basePath, BiConsumer<Path, Path> onWalk) throws IOException {
		Path path = resolve(basePath);
		try (Stream<Path> walk = Files.walk(path)) {
			walk.forEach(p -> onWalk.accept(p, path.relativize(p)));
		}
	}
}
