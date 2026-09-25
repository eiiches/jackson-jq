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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.RuntimeLimits;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.ArrayType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class ListFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final Type ENTRY = ObjectType.of("path", StringType.getInstance(), "type", StringType.getInstance());
	private static final Type OPTIONS = ObjectType.of(
			"recursive", FileFunctionSupport.OPTIONAL_BOOLEAN,
			"follow_symlinks", FileFunctionSupport.OPTIONAL_BOOLEAN);
	/**
	 * Indexed by argument count; index 0 is unused because the directory is required. The input is only
	 * passed on to the arguments, so its type flows through untouched.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, ArrayType.of(ENTRY), FilterType.of(INPUT, StringType.getInstance())))),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, ArrayType.of(ENTRY), FilterType.of(INPUT, StringType.getInstance()), FilterType.of(INPUT, OPTIONS)))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 2)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		boolean input = arguments.stream().anyMatch(ExpressionProperties::dependsOnInput);
		Cardinality first = arguments.get(0).cardinality();
		if (arguments.size() == 1 || first == Cardinality.ZERO)
			return new ExpressionProperties(first, input, true);
		Cardinality second = arguments.get(1).cardinality();
		Cardinality cardinality = second == Cardinality.ZERO ? Cardinality.ZERO
				: first == Cardinality.ONE && second == Cardinality.ONE ? Cardinality.ONE : Cardinality.UNKNOWN;
		return new ExpressionProperties(cardinality, input, true);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> pathExpression = arguments.get(0);
		Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		return new Expression<>() {


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
			List<Entry> entries = options.recursive()
					? listRecursively(directory, options.followSymlinks(), limits)
					: listDirectly(directory, limits);
			entries.sort(Comparator.comparing(Entry::path));
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
		Files.walkFileTree(directory, options, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
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
		RuntimeLimitChecks.checkStringLength(limits, entry.path().length());
		RuntimeLimitChecks.checkStringLength(limits, entry.type().length());
		Map<String, JsonNode> value = new LinkedHashMap<>();
		value.put("path", jsonProvider.createString(entry.path()));
		value.put("type", jsonProvider.createString(entry.type()));
		return jsonProvider.createObject(value);
	}

	private record Options(boolean recursive, boolean followSymlinks) {
	}

	private record Entry(String path, String type) {
	}
}
