package net.thisptr.jackson.jq.v2.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ClassLoaderUtils {
	private ClassLoaderUtils() {
	}

	public static List<String> listResources(ClassLoader classLoader, String basePath) throws IOException {
		URL resource = Objects.requireNonNull(classLoader.getResource(basePath), "Resource not found: " + basePath);
		try {
			if ("file".equals(resource.getProtocol()))
				return listResources(Paths.get(resource.toURI()), basePath);
			if ("jar".equals(resource.getProtocol()))
				return listJarResources(resource, basePath);
			throw new IOException("Unsupported resource protocol: " + resource.getProtocol());
		} catch (URISyntaxException e) {
			throw new IOException("Invalid resource URI: " + resource, e);
		}
	}

	public static void copyResources(ClassLoader classLoader, String basePath, Path destination) throws IOException {
		for (String resourceName : listResources(classLoader, basePath)) {
			Path relativePath = Paths.get(resourceName.substring(basePath.length() + 1));
			Path output = destination.resolve(relativePath.toString());
			Files.createDirectories(Objects.requireNonNull(output.getParent()));
			try (InputStream in = Objects.requireNonNull(
					classLoader.getResourceAsStream(resourceName), "Resource not found: " + resourceName)) {
				Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	// Synchronized to prevent a race condition during parallel test execution: without synchronization,
	// one thread may close the ZipFileSystem upon exiting try-with-resources while another thread
	// is concurrently reading from it, causing ClosedFileSystemException.
	private static synchronized List<String> listJarResources(URL resource, String basePath) throws IOException, URISyntaxException {
		JarURLConnection connection = (JarURLConnection) resource.openConnection();
		URI fileSystemUri = URI.create("jar:" + connection.getJarFileURL().toURI());
		try {
			try (FileSystem fileSystem = FileSystems.newFileSystem(fileSystemUri, Collections.emptyMap())) {
				return listResources(fileSystem.getPath("/" + connection.getEntryName()), basePath);
			}
		} catch (FileSystemAlreadyExistsException e) {
			FileSystem fileSystem = FileSystems.getFileSystem(fileSystemUri);
			return listResources(fileSystem.getPath("/" + connection.getEntryName()), basePath);
		}
	}

	private static List<String> listResources(Path root, String basePath) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
					.map(root::relativize)
					.map(path -> basePath + "/" + path.toString().replace('\\', '/'))
					.collect(Collectors.toList());
		}
	}
}
