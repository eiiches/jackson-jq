import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiReleaseJarTest {

	@TempDir
	Path tempDir;

	private Path createJar(String name, Manifest manifest, String... entryNameAndContents) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		for (int i = 0; i < entryNameAndContents.length; i += 2) {
			entries.put(entryNameAndContents[i], entryNameAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
		}
		return createJar(name, manifest, entries);
	}

	private Path createJar(String name, Manifest manifest, Map<String, byte[]> entries) throws IOException {
		Path jarPath = tempDir.resolve(name);
		try (OutputStream out = Files.newOutputStream(jarPath);
			 JarOutputStream jar = new JarOutputStream(out)) {
			if (manifest != null) {
				JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
				jar.putNextEntry(manifestEntry);
				manifest.write(jar);
				jar.closeEntry();
			}
			for (Map.Entry<String, byte[]> jarEntry : entries.entrySet()) {
				JarEntry entry = new JarEntry(jarEntry.getKey());
				jar.putNextEntry(entry);
				jar.write(jarEntry.getValue());
				jar.closeEntry();
			}
		}
		return jarPath;
	}

	private static byte[] moduleInfo(Class<?> type) throws IOException {
		try (InputStream in = type.getModule().getResourceAsStream("module-info.class")) {
			if (in == null)
				throw new IOException("module-info.class is missing from " + type.getModule().getName());
			return in.readAllBytes();
		}
	}

	private String readEntry(Path jarPath, String entryName) throws IOException {
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			JarEntry entry = jar.getJarEntry(entryName);
			if (entry == null)
				return null;
			try (InputStream in = jar.getInputStream(entry);
				 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
				}
				return sb.toString();
			}
		}
	}

	@Test
	void mergesInputsAndServices() throws IOException {
		Path pkg1 = createJar("pkg1.jar", null,
				"net/thisptr/A.class", "class A",
				"META-INF/services/net.thisptr.Service", "net.thisptr.Impl1\nnet.thisptr.Shared\n");
		Path pkg2 = createJar("pkg2.jar", null,
				"net/thisptr/B.class", "class B",
				"META-INF/services/net.thisptr.Service", "net.thisptr.Shared\nnet.thisptr.Impl2\n");

		Path output = tempDir.resolve("out.jar");
		MultiReleaseJar.main(new String[] {
				"--output", output.toString(),
				"--input", pkg1.toString(),
				"--input", pkg2.toString(),
		});

		assertThat(readEntry(output, "net/thisptr/A.class")).isEqualTo("class A\n");
		assertThat(readEntry(output, "net/thisptr/B.class")).isEqualTo("class B\n");
		assertThat(readEntry(output, "META-INF/services/net.thisptr.Service"))
				.isEqualTo("net.thisptr.Impl1\nnet.thisptr.Shared\nnet.thisptr.Impl2\n");
	}

	@Test
	void mergesOverlaysAndSetsMultiRelease() throws IOException {
		Path base = createJar("base.jar", null,
				"net/thisptr/A.class", "class A v8");
		Path overlay = createJar("overlay.jar", null, Map.of("module-info.class", moduleInfo(Object.class)));

		Path output = tempDir.resolve("out-mr.jar");
		MultiReleaseJar.main(new String[] {
				"--output", output.toString(),
				"--base", base.toString(),
				"--overlay", "9=" + overlay.toString(),
				"--multi-release",
		});

		assertThat(readEntry(output, "net/thisptr/A.class")).isEqualTo("class A v8\n");
		assertThat(readEntry(output, "META-INF/services/java.nio.file.spi.FileSystemProvider"))
				.isEqualTo("jdk.internal.jrtfs.JrtFileSystemProvider\n");

		try (JarFile jar = new JarFile(output.toFile())) {
			Manifest manifest = jar.getManifest();
			assertThat(manifest).isNotNull();
			assertThat(manifest.getMainAttributes().getValue("Multi-Release")).isEqualTo("true");
		}
	}

	@Test
	void addsDeclaredProvidersToExistingServices() {
		ModuleDescriptor descriptor = ModuleDescriptor.newModule("net.thisptr.sample")
				.provides("net.thisptr.zeta.Service", List.of("net.thisptr.ZetaImpl"))
				.provides("net.thisptr.alpha.Service", List.of("net.thisptr.FirstImpl", "net.thisptr.SecondImpl"))
				.build();
		Map<String, Set<String>> services = new LinkedHashMap<>();
		services.put("META-INF/services/net.thisptr.alpha.Service",
				new LinkedHashSet<>(List.of("net.thisptr.ExistingImpl", "net.thisptr.FirstImpl")));

		MultiReleaseJar.addModuleServices(descriptor, services);

		assertThat(services.keySet()).containsExactly(
				"META-INF/services/net.thisptr.alpha.Service",
				"META-INF/services/net.thisptr.zeta.Service");
		assertThat(services.get("META-INF/services/net.thisptr.alpha.Service"))
				.containsExactly("net.thisptr.ExistingImpl", "net.thisptr.FirstImpl", "net.thisptr.SecondImpl");
		assertThat(services.get("META-INF/services/net.thisptr.zeta.Service"))
				.containsExactly("net.thisptr.ZetaImpl");
	}

	@Test
	void noManifestOmitsManifest() throws IOException {
		Path pkg = createJar("pkg.jar", null, "A.txt", "hello");
		Path output = tempDir.resolve("out-no-manifest.jar");
		MultiReleaseJar.main(new String[] {
				"--output", output.toString(),
				"--input", pkg.toString(),
				"--no-manifest",
		});

		try (JarFile jar = new JarFile(output.toFile())) {
			assertThat(jar.getJarEntry(JarFile.MANIFEST_NAME)).isNull();
		}
	}
}
