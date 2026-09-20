package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import net.thisptr.jackson.jq.v2.json.JsonException;
import net.thisptr.jackson.jq.v2.json.JsonParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.Maybe;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public final class JsonReadFunction implements Function {
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
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> pathExpression = arguments.get(0);
		Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		return new Expression<>() {
			@Override
			public Cardinality getCardinality() {
				Cardinality pathCardinality = pathExpression.getCardinality();
				if (pathCardinality == Cardinality.ZERO)
					return Cardinality.ZERO;
				if (optionsExpression != null && optionsExpression.getCardinality() == Cardinality.ZERO)
					return Cardinality.ZERO;
				if (!stream && (optionsExpression == null || optionsExpression.getCardinality() == Cardinality.ONE) && pathCardinality == Cardinality.ONE)
					return Cardinality.ONE;
				return Cardinality.UNKNOWN;
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
