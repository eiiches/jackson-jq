package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.errorprone.annotations.Var;

/**
 * Resolves the test-case files a tool was asked to read, and whether it may write them.
 */
public final class TestCaseFiles {
	/**
	 * A command line: the files to work on, and whether {@code --check} asked for a dry run.
	 *
	 * @param check whether to report what would change instead of changing it
	 * @param files the YAML files to work on
	 */
	public record Invocation(boolean check, List<Path> files) {
	}

	/**
	 * Parses a command line of {@code --check} and paths, each a YAML file or a directory of them.
	 *
	 * <p>Relative paths resolve against the directory {@code bazel run} was invoked from, which is
	 * where the caller typed them. At least one path is required: a tool that defaulted to the whole
	 * corpus would rewrite every file of it on a run aimed at one.
	 *
	 * @param args the command line as given
	 * @param tool the tool's target name, for the usage message
	 * @return the files to work on, directories expanded and sorted, and the {@code --check} flag
	 * @throws IOException if a directory cannot be walked
	 */
	public static Invocation parse(String[] args, String tool) throws IOException {
		@Var boolean check = false;
		List<String> paths = new ArrayList<>();
		for (String arg : args) {
			if (arg.equals("--check")) {
				check = true;
			} else {
				paths.add(arg);
			}
		}
		return new Invocation(check, resolve(paths, tool));
	}

	private static List<Path> resolve(List<String> paths, String tool) throws IOException {
		if (paths.isEmpty())
			throw new IllegalArgumentException("Usage: bazelisk run //:" + tool + " -- [--check] <test-case.yaml|directory>...");
		String workingDirectory = System.getenv("BUILD_WORKING_DIRECTORY");
		Path base = workingDirectory != null && !workingDirectory.isBlank() ? Path.of(workingDirectory) : Path.of("");
		List<Path> files = new ArrayList<>();
		for (String path : paths) {
			Path resolved = base.resolve(path);
			if (Files.isDirectory(resolved)) {
				try (Stream<Path> stream = Files.walk(resolved)) {
					stream.filter(p -> p.toString().endsWith(".yaml")).forEach(files::add);
				}
			} else if (Files.isRegularFile(resolved)) {
				files.add(resolved);
			} else {
				throw new IllegalArgumentException("no such file or directory: " + resolved);
			}
		}
		Collections.sort(files);
		return files;
	}

	private TestCaseFiles() {
	}
}
