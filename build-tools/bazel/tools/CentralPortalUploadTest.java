import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the parts of the uploader that need neither gpg nor the network: which staged files are
 * signed, which end up in the bundle, and what a missing signature reports.
 */
class CentralPortalUploadTest {

	private static final String GROUP = "net/thisptr/jackson-jq-core/2.0.0/";

	private static final String JAR = GROUP + "jackson-jq-core-2.0.0.jar";

	private static final String POM = GROUP + "jackson-jq-core-2.0.0.pom";

	@TempDir
	Path repository;

	@BeforeEach
	void stageRepository() throws IOException {
		stage(JAR);
		stage(JAR + ".md5");
		stage(JAR + ".sha1");
		stage(JAR + ".sha256");
		stage(JAR + ".sha512");
		stage(POM);
		stage(POM + ".sha1");
		stage(GROUP + "maven-metadata.xml");
		stage("net/thisptr/jackson-jq-core/maven-metadata.xml");
		stage("net/thisptr/jackson-jq-core/maven-metadata.xml.sha1");
	}

	@Test
	void publishableFilesSkipsMetadataChecksumsAndSignatures() throws IOException {
		stage(JAR + ".asc");

		assertThat(relative(CentralPortalUpload.publishableFiles(repository)))
				.containsExactly(JAR, POM);
	}

	@Test
	void bundleFilesKeepsChecksumsAndSignaturesButNotMetadata() throws IOException {
		stage(JAR + ".asc");
		stage(POM + ".asc");

		assertThat(relative(CentralPortalUpload.bundleFiles(repository)))
				.containsExactly(
						JAR,
						JAR + ".asc",
						JAR + ".md5",
						JAR + ".sha1",
						JAR + ".sha256",
						JAR + ".sha512",
						POM,
						POM + ".asc",
						POM + ".sha1");
	}

	@Test
	void verifySignedNamesEveryUnsignedFile() {
		assertThatThrownBy(() -> CentralPortalUpload.verifySigned(repository))
				.isInstanceOf(CentralPortalUpload.Failure.class)
				.hasMessage("No signature for 2 staged file(s):\n  jackson-jq-core-2.0.0.jar\n  jackson-jq-core-2.0.0.pom");
	}

	@Test
	void verifySignedAcceptsAFullySignedRepository() throws IOException {
		stage(JAR + ".asc");
		stage(POM + ".asc");

		CentralPortalUpload.verifySigned(repository);
	}

	@Test
	void createBundleWritesRepositoryRelativeEntries() throws IOException {
		stage(JAR + ".asc");
		stage(POM + ".asc");
		Path bundle = repository.resolveSibling("central-bundle.zip");

		CentralPortalUpload.createBundle(repository, bundle);

		assertThat(entries(bundle)).containsExactlyElementsOf(relative(CentralPortalUpload.bundleFiles(repository)));
	}

	@Test
	void createBundlePreservesFileContent() throws IOException {
		stage(JAR + ".asc");
		stage(POM + ".asc");
		Path bundle = repository.resolveSibling("central-bundle.zip");

		CentralPortalUpload.createBundle(repository, bundle);

		assertThat(read(bundle, JAR)).isEqualTo(JAR);
	}

	private void stage(String path) throws IOException {
		Path artifact = repository.resolve(path);
		Files.createDirectories(artifact.getParent());
		// Content unique per path, so the bundle can be checked entry by entry.
		Files.write(artifact, path.getBytes(StandardCharsets.UTF_8));
	}

	private List<String> relative(List<Path> artifacts) {
		List<String> names = new ArrayList<>();
		for (Path artifact : artifacts)
			names.add(repository.relativize(artifact).toString());
		return names;
	}

	private static List<String> entries(Path bundle) throws IOException {
		List<String> names = new ArrayList<>();
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(bundle))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				names.add(entry.getName());
		}
		return names;
	}

	private static String read(Path bundle, String name) throws IOException {
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(bundle))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				if (name.equals(entry.getName()))
					return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
		}
		throw new AssertionError("no such bundle entry: " + name);
	}
}
