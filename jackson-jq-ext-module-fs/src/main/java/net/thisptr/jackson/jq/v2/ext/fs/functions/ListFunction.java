package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public final class ListFunction implements Function {
	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> pathExpression = arguments.get(0);
		@Nullable Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		return new Expression<Context, JsonNode>() {
			@Override
			public Cardinality getCardinality() {
				Cardinality pathCardinality = pathExpression.getCardinality();
				if (optionsExpression == null || pathCardinality == Cardinality.ZERO)
					return pathCardinality;
				Cardinality optionsCardinality = optionsExpression.getCardinality();
				if (optionsCardinality == Cardinality.ZERO)
					return Cardinality.ZERO;
				return pathCardinality == Cardinality.ONE && optionsCardinality == Cardinality.ONE ? Cardinality.ONE : Cardinality.UNKNOWN;
			}

			@Override
			public boolean dependsOnInput() {
				return pathExpression.dependsOnInput() || (optionsExpression != null && optionsExpression.dependsOnInput());
			}

			@Override
			public boolean dependsOnExternalState() {
				return true;
			}

			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path directory = FileFunctionSupport.parsePath(jsonProvider, pathNode, "fs::list");
					if (optionsExpression == null) {
						output.emit(list(jsonProvider, context.getRuntimeLimits(), directory, new Options(false, true)), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						Options options = parseOptions(jsonProvider, optionsNode);
						output.emit(list(jsonProvider, context.getRuntimeLimits(), directory, options), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> Options parseOptions(JsonProvider<JsonNode> jsonProvider, JsonNode node) {
		JsonNodeType type = jsonProvider.getNodeType(node);
		if (type != JsonNodeType.OBJECT)
			throw new JsonQueryException("fs::list options must be an object, but got " + type);
		Iterator<String> names = jsonProvider.getObjectMemberNames(node);
		while (names.hasNext()) {
			String name = names.next();
			if (!name.equals("recursive") && !name.equals("follow_symlinks"))
				throw new JsonQueryException("fs::list options contains unknown member: " + name);
		}
		boolean recursive = booleanOption(jsonProvider, node, "recursive", false);
		boolean followSymlinks = booleanOption(jsonProvider, node, "follow_symlinks", true);
		return new Options(recursive, followSymlinks);
	}

	private static <JsonNode> boolean booleanOption(JsonProvider<JsonNode> jsonProvider, JsonNode options, String name, boolean defaultValue) {
		if (!jsonProvider.hasObjectMember(options, name))
			return defaultValue;
		JsonNode value = jsonProvider.getObjectMemberOrThrow(options, name);
		if (!jsonProvider.isBoolean(value))
			throw new JsonQueryException("fs::list " + name + " must be a boolean");
		return jsonProvider.getBoolean(value);
	}

	private static <JsonNode> JsonNode list(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, java.nio.file.Path requestedDirectory, Options options) {
		try {
			if (!Files.isDirectory(requestedDirectory))
				throw new JsonQueryException("fs::list path is not a directory: " + requestedDirectory);
			java.nio.file.Path directory = Files.isSymbolicLink(requestedDirectory) ? requestedDirectory.toRealPath() : requestedDirectory;
			List<Entry> entries = options.recursive
					? listRecursively(directory, options.followSymlinks, limits)
					: listDirectly(directory, limits);
			Collections.sort(entries, Comparator.comparing(entry -> entry.path));
			List<JsonNode> result = new ArrayList<>(entries.size());
			for (Entry entry : entries)
				result.add(createEntry(jsonProvider, limits, entry));
			return jsonProvider.createArray(result);
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException("fs::list failed for " + requestedDirectory + ": " + e.getMessage(), e);
		}
	}

	private static List<Entry> listDirectly(java.nio.file.Path directory, RuntimeLimits limits) throws IOException {
		List<Entry> result = new ArrayList<>();
		try (java.nio.file.DirectoryStream<java.nio.file.Path> children = Files.newDirectoryStream(directory)) {
			for (java.nio.file.Path child : children)
				add(result, limits, new Entry(directory.relativize(child).toString(), typeOf(child)));
		}
		return result;
	}

	private static List<Entry> listRecursively(java.nio.file.Path directory, boolean followSymlinks, RuntimeLimits limits) throws IOException {
		List<Entry> result = new ArrayList<>();
		EnumSet<FileVisitOption> options = followSymlinks ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class);
		Files.walkFileTree(directory, options, Integer.MAX_VALUE, new SimpleFileVisitor<java.nio.file.Path>() {
			@Override
			public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) {
				if (!dir.equals(directory))
					add(result, limits, new Entry(directory.relativize(dir).toString(), typeOf(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
				add(result, limits, new Entry(directory.relativize(file).toString(), typeOf(file)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(java.nio.file.Path file, IOException failure) throws IOException {
				if (failure instanceof FileSystemLoopException && Files.isSymbolicLink(file)) {
					add(result, limits, new Entry(directory.relativize(file).toString(), "symlink"));
					return FileVisitResult.SKIP_SUBTREE;
				}
				throw failure;
			}
		});
		return result;
	}

	private static void add(List<Entry> entries, RuntimeLimits limits, Entry entry) {
		RuntimeLimitChecks.checkArrayLength(limits, (long) entries.size() + 1);
		entries.add(entry);
	}

	private static String typeOf(java.nio.file.Path path) {
		if (Files.isSymbolicLink(path))
			return "symlink";
		if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
			return "file";
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
			return "directory";
		return "other";
	}

	private static <JsonNode> JsonNode createEntry(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, Entry entry) {
		RuntimeLimitChecks.checkObjectLength(limits, 2);
		RuntimeLimitChecks.checkStringLength(limits, entry.path.length());
		RuntimeLimitChecks.checkStringLength(limits, entry.type.length());
		Map<String, JsonNode> value = new LinkedHashMap<>();
		value.put("path", jsonProvider.createString(entry.path));
		value.put("type", jsonProvider.createString(entry.type));
		return jsonProvider.createObject(value);
	}

	private static final class Options {
		private final boolean followSymlinks;
		private final boolean recursive;

		private Options(boolean recursive, boolean followSymlinks) {
			this.recursive = recursive;
			this.followSymlinks = followSymlinks;
		}
	}

	private static final class Entry {
		private final String path;
		private final String type;

		private Entry(String path, String type) {
			this.path = path;
			this.type = type;
		}
	}
}
