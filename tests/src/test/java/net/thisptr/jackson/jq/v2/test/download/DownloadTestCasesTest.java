package net.thisptr.jackson.jq.v2.test.download;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.thisptr.jackson.jq.v2.spi.version.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DownloadTestCasesTest {
	@TempDir
	Path directory;

	@Test
	void acceptsLongOptions() throws Exception {
		DownloadTestCases.Arguments arguments = DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.8.2, )", "--output-directory", directory.toString()
		});
		assertThat(arguments.versions()).containsExactly(Version.of(1, 8, 2));
		assertThat(arguments.directory()).isEqualTo(directory);
	}

	@Test
	void acceptsShortOutputDirectoryOption() throws Exception {
		DownloadTestCases.Arguments arguments = DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.5, 1.6)", "-o", directory.toString()
		});
		assertThat(arguments.versions()).containsExactly(Version.of(1, 5));
		assertThat(arguments.directory()).isEqualTo(directory);
	}

	@Test
	void selectsAllSupportedVersionsInRange() throws Exception {
		DownloadTestCases.Arguments arguments = DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.7, 1.8.0)", "-o", directory.toString()
		});
		assertThat(arguments.versions()).isEqualTo(List.of(Version.of(1, 7), Version.of(1, 7, 1)));
	}

	@Test
	void rejectsMissingAndUnsupportedOptions() {
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] { "--jq-version", "[1.8.2, )" }))
				.hasMessageContaining("Missing required option: o");
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] { "-o", directory.toString() }))
				.hasMessageContaining("Missing required option: jq-version");
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.8.3, )", "-o", directory.toString()
		}))
				.hasMessageContaining("selects no supported releases");
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] {
				"--jq-version", "latest", "-o", directory.toString()
		}))
				.hasMessageContaining("Invalid VersionRange");
	}

	@Test
	void rejectsPositionalArgumentsAndMissingDirectory() {
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.8.2, )", "-o", directory.toString(), "extra"
		}))
				.hasMessageContaining("unexpected positional arguments");
		assertThatThrownBy(() -> DownloadTestCases.arguments(new String[] {
				"--jq-version", "[1.8.2, )", "-o", directory.resolve("missing").toString()
		}))
				.hasMessageContaining("no such directory");
	}
}
