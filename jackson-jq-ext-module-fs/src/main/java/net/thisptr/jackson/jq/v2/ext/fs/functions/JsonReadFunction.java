package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.ExpressionProperties;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class JsonReadFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	/**
	 * Reading JSON takes no options, so the options object must be empty.
	 */
	private static final Type OPTIONS = ObjectType.of();
	/**
	 * Indexed by argument count; index 0 is unused because the path is required. What a file holds is
	 * not known until it is read, so the output is unconstrained.
	 */
	private static final List<List<TypeScheme<FunctionType>>> TYPE_SCHEMES = List.of(
			List.of(),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, AnyType.getInstance(), FilterType.of(INPUT, StringType.getInstance())))),
			List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, AnyType.getInstance(), FilterType.of(INPUT, StringType.getInstance()), FilterType.of(INPUT, OPTIONS)))));

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments > 2)
			return List.of();
		return TYPE_SCHEMES.get(totalArguments);
	}

	private final boolean stream;

	private JsonReadFunction(boolean stream) {
		this.stream = stream;
	}

	public static JsonReadFunction single() {
		return new JsonReadFunction(false);
	}

	public static JsonReadFunction stream() {
		return new JsonReadFunction(true);
	}

	@Override
	public ExpressionProperties analyze(Version jqVersion, List<ExpressionProperties> arguments) {
		boolean input = arguments.stream().anyMatch(ExpressionProperties::dependsOnInput);
		Cardinality path = arguments.get(0).cardinality();
		if (path == Cardinality.ZERO || (arguments.size() == 2 && arguments.get(1).cardinality() == Cardinality.ZERO))
			return new ExpressionProperties(Cardinality.ZERO, input, true);
		if (!stream && path == Cardinality.ONE && (arguments.size() == 1 || arguments.get(1).cardinality() == Cardinality.ONE))
			return new ExpressionProperties(Cardinality.ONE, input, true);
		return new ExpressionProperties(Cardinality.UNKNOWN, input, true);
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
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, functionName());
					if (optionsExpression == null) {
						if (stream)
							readStream(jsonProvider, file, output);
						else
							readSingle(jsonProvider, file, output);
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, functionName(), Collections.emptySet());
						if (stream)
							readStream(jsonProvider, file, output);
						else
							readSingle(jsonProvider, file, output);
					});
				});
			}
		};
	}

	private String functionName() {
		return stream ? "fs::read_json_stream" : "fs::read_json";
	}

	private <JsonNode> void readSingle(JsonProvider<JsonNode> jsonProvider, java.nio.file.Path file, Output<JsonNode> output) {
		try (InputStream in = Files.newInputStream(file);
			 JsonParser<JsonNode> parser = jsonProvider.createParser(in)) {
			Maybe<JsonNode> first;
			try {
				first = parser.next();
			} catch (JsonException e) {
				throw new JsonQueryException("fs::read_json failed for " + file + ": " + e.getMessage(), e);
			}
			if (first.isAbsent())
				throw new JsonQueryException("fs::read_json failed for " + file + ": empty input");
			Maybe<JsonNode> second;
			try {
				second = parser.next();
			} catch (JsonException e) {
				throw new JsonQueryException("fs::read_json failed for " + file + ": " + e.getMessage(), e);
			}
			if (second.isPresent())
				throw new JsonQueryException("fs::read_json failed for " + file + ": trailing content");
			output.emit(first.get(), UntrackedPath.getInstance());
		} catch (JsonQueryException e) {
			throw e;
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException("fs::read_json failed for " + file + ": " + e.getMessage(), e);
		}
	}

	private <JsonNode> void readStream(JsonProvider<JsonNode> jsonProvider, java.nio.file.Path file, Output<JsonNode> output) {
		try (InputStream in = Files.newInputStream(file);
			 JsonParser<JsonNode> parser = jsonProvider.createParser(in)) {
			while (true) {
				Maybe<JsonNode> next;
				try {
					next = parser.next();
				} catch (JsonException e) {
					throw new JsonQueryException("fs::read_json_stream failed for " + file + ": " + e.getMessage(), e);
				}
				if (next.isAbsent())
					break;
				output.emit(next.get(), UntrackedPath.getInstance());
			}
		} catch (JsonQueryException e) {
			throw e;
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException("fs::read_json_stream failed for " + file + ": " + e.getMessage(), e);
		}
	}
}
