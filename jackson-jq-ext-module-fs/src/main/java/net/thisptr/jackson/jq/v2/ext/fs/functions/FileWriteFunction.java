package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;

public final class FileWriteFunction implements Function {
	private static final Set<String> TEXT_ALLOWED_OPTIONS = new HashSet<>(Arrays.asList("encoding", "append", "create_parents", "mkdirs"));
	private static final Set<String> BINARY_ALLOWED_OPTIONS = new HashSet<>(Arrays.asList("append", "create_parents", "mkdirs"));

	private final boolean binary;

	private FileWriteFunction(boolean binary) {
		this.binary = binary;
	}

	public static FileWriteFunction text() {
		return new FileWriteFunction(false);
	}

	public static FileWriteFunction binary() {
		return new FileWriteFunction(true);
	}

	@Override
	public <Context extends RuntimeContext, JsonNode> Expression<Context, JsonNode> bind(BindContext<JsonNode> bindContext, List<Expression<Context, JsonNode>> arguments) {
		JsonProvider<JsonNode> jsonProvider = bindContext.getJsonProvider();
		Expression<Context, JsonNode> pathExpression = arguments.get(0);
		@Nullable Expression<Context, JsonNode> optionsExpression = arguments.size() == 2 ? arguments.get(1) : null;
		String function = binary ? "fs::write_binary" : "fs::write_text";
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
				return true;
			}

			@Override
			public boolean dependsOnExternalState() {
				return true;
			}

			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				if (binary) {
					byte[] bytes = getBinaryInput(jsonProvider, input, function);
					pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
						java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, function);
						if (optionsExpression == null) {
							FileFunctionSupport.write(file, bytes, false, false, function);
							output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
							return;
						}
						optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
							BinaryOptions options = parseBinaryOptions(jsonProvider, optionsNode, function);
							FileFunctionSupport.write(file, bytes, options.append, options.createParents, function);
							output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						});
					});
					return;
				}

				String text = getTextInput(jsonProvider, input, function);
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, function);
					if (optionsExpression == null) {
						FileFunctionSupport.write(file, encode(text, StandardCharsets.UTF_8, function), false, false, function);
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						TextOptions options = parseTextOptions(jsonProvider, optionsNode, function);
						byte[] bytes = encode(text, options.charset, function);
						FileFunctionSupport.write(file, bytes, options.append, options.createParents, function);
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> TextOptions parseTextOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode, String function) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, function, TEXT_ALLOWED_OPTIONS);
		Charset charset = FileFunctionSupport.parseEncodingOption(jsonProvider, optionsNode, function);
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, function, "append", false);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, function);
		return new TextOptions(charset, append, createParents);
	}

	private static <JsonNode> BinaryOptions parseBinaryOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode, String function) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, function, BINARY_ALLOWED_OPTIONS);
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, function, "append", false);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, function);
		return new BinaryOptions(append, createParents);
	}

	private static <JsonNode> String getTextInput(JsonProvider<JsonNode> jsonProvider, JsonNode input, String function) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires string input, but got " + type);
		return jsonProvider.getString(input);
	}

	private static <JsonNode> byte[] getBinaryInput(JsonProvider<JsonNode> jsonProvider, JsonNode input, String function) {
		JsonNodeType type = jsonProvider.getNodeType(input);
		if (type == JsonNodeType.BINARY)
			return jsonProvider.getBinaryAsByteArray(input);
		if (type != JsonNodeType.STRING)
			throw new JsonQueryException(function + " requires binary or Base64 string input, but got " + type);
		try {
			return Base64.getDecoder().decode(jsonProvider.getString(input));
		} catch (IllegalArgumentException e) {
			throw new JsonQueryException(function + " input must be valid Base64", e);
		}
	}

	private static byte[] encode(String text, Charset charset, String function) {
		try {
			ByteBuffer encoded = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new JsonQueryException(function + " failed to encode the input using " + charset.name() + ": " + e.getMessage(), e);
		}
	}

	private static final class TextOptions {
		private final Charset charset;
		private final boolean append;
		private final boolean createParents;

		private TextOptions(Charset charset, boolean append, boolean createParents) {
			this.charset = charset;
			this.append = append;
			this.createParents = createParents;
		}
	}

	private static final class BinaryOptions {
		private final boolean append;
		private final boolean createParents;

		private BinaryOptions(boolean append, boolean createParents) {
			this.append = append;
			this.createParents = createParents;
		}
	}
}
