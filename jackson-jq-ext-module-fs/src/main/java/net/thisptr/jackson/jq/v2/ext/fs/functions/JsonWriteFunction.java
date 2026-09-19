package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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

public final class JsonWriteFunction implements Function {
	private static final Set<String> ALLOWED_OPTIONS = new HashSet<>(Arrays.asList("indent", "encoding", "append", "newline", "create_parents", "mkdirs"));
	private static final Options DEFAULT_OPTIONS = new Options(null, StandardCharsets.UTF_8, false, true, false);

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
				return true;
			}

			@Override
			public boolean dependsOnExternalState() {
				return true;
			}

			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, "fs::write_json");
					if (optionsExpression == null) {
						byte[] bytes = formatAndEncode(jsonProvider, input, DEFAULT_OPTIONS);
						FileFunctionSupport.write(file, bytes, false, false, "fs::write_json");
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						Options options = parseOptions(jsonProvider, optionsNode);
						byte[] bytes = formatAndEncode(jsonProvider, input, options);
						FileFunctionSupport.write(file, bytes, options.append, options.createParents, "fs::write_json");
						output.emit(jsonProvider.createNull(), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private static <JsonNode> Options parseOptions(JsonProvider<JsonNode> jsonProvider, JsonNode optionsNode) {
		FileFunctionSupport.checkAllowedOptionMembers(jsonProvider, optionsNode, "fs::write_json", ALLOWED_OPTIONS);
		String indent = FileFunctionSupport.parseIndentOption(jsonProvider, optionsNode, "fs::write_json");
		Charset encoding = FileFunctionSupport.parseEncodingOption(jsonProvider, optionsNode, "fs::write_json");
		boolean append = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, "fs::write_json", "append", false);
		boolean newline = FileFunctionSupport.parseBooleanOption(jsonProvider, optionsNode, "fs::write_json", "newline", true);
		boolean createParents = FileFunctionSupport.parseCreateParentsOption(jsonProvider, optionsNode, "fs::write_json");
		return new Options(indent, encoding, append, newline, createParents);
	}

	private static <JsonNode> byte[] formatAndEncode(JsonProvider<JsonNode> jsonProvider, JsonNode input, Options options) {
		String formatted = JsonPrettyPrinter.print(jsonProvider, input, options.indent);
		String text = options.newline ? formatted + "\n" : formatted;
		return encode(text, options.encoding);
	}

	private static byte[] encode(String text, Charset charset) {
		try {
			ByteBuffer encoded = charset.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[encoded.remaining()];
			encoded.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {
			throw new JsonQueryException("fs::write_json failed to encode the input using " + charset.name() + ": " + e.getMessage(), e);
		}
	}

	private static final class Options {
		private final @Nullable String indent;
		private final Charset encoding;
		private final boolean append;
		private final boolean newline;
		private final boolean createParents;

		private Options(@Nullable String indent, Charset encoding, boolean append, boolean newline, boolean createParents) {
			this.indent = indent;
			this.encoding = encoding;
			this.append = append;
			this.newline = newline;
			this.createParents = createParents;
		}
	}
}
