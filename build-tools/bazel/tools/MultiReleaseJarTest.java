import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
		Path jarPath = tempDir.resolve(name);
		try (OutputStream out = Files.newOutputStream(jarPath);
			 JarOutputStream jar = new JarOutputStream(out)) {
			if (manifest != null) {
				JarEntry manifestEntry = new JarEntry(JarFile.MANIFEST_NAME);
				jar.putNextEntry(manifestEntry);
				manifest.write(jar);
				jar.closeEntry();
			}
			for (int i = 0; i < entryNameAndContents.length; i += 2) {
				String entryName = entryNameAndContents[i];
				String content = entryNameAndContents[i + 1];
				JarEntry entry = new JarEntry(entryName);
				jar.putNextEntry(entry);
				jar.write(content.getBytes(StandardCharsets.UTF_8));
				jar.closeEntry();
			}
		}
		return jarPath;
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
		Path overlay = createJar("overlay.jar", null,
				"module-info.class", "module-info bytecode");

		Path output = tempDir.resolve("out-mr.jar");
		MultiReleaseJar.main(new String[] {
				"--output", output.toString(),
				"--base", base.toString(),
				"--overlay", "9=" + overlay.toString(),
				"--multi-release",
		});

		assertThat(readEntry(output, "net/thisptr/A.class")).isEqualTo("class A v8\n");
		assertThat(readEntry(output, "META-INF/versions/9/module-info.class")).isEqualTo("module-info bytecode\n");

		try (JarFile jar = new JarFile(output.toFile())) {
			Manifest manifest = jar.getManifest();
			assertThat(manifest).isNotNull();
			assertThat(manifest.getMainAttributes().getValue("Multi-Release")).isEqualTo("true");
		}
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
