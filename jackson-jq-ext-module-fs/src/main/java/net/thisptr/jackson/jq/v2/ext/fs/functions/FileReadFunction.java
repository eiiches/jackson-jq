package net.thisptr.jackson.jq.v2.ext.fs.functions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
import net.thisptr.jackson.jq.v2.spi.exception.RuntimeLimitExceededException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.type.AnyType;
import net.thisptr.jackson.jq.v2.spi.type.BinaryType;
import net.thisptr.jackson.jq.v2.spi.type.FilterType;
import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.ObjectType;
import net.thisptr.jackson.jq.v2.spi.type.StringType;
import net.thisptr.jackson.jq.v2.spi.type.Type;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.type.TypeVariable;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public final class FileReadFunction implements Function {
	private static final TypeVariable INPUT = TypeVariable.of("Input");
	private static final Type OPTIONS = ObjectType.of("encoding", FileFunctionSupport.OPTIONAL_STRING);

	/**
	 * Indexed by argument count; index 0 is unused because the path is required. Reading bytes takes no
	 * charset, so {@code read_binary} is registered at arity 1 alone. The input is only passed on to the
	 * arguments, so its type flows through untouched.
	 */
	private final List<List<TypeScheme<FunctionType>>> typeSchemes;
	private final boolean binary;

	private FileReadFunction(boolean binary) {
		this.binary = binary;
		Type outputType = binary ? BinaryType.getInstance() : StringType.getInstance();
		List<TypeScheme<FunctionType>> withPathOnly = List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()),
				FunctionType.of(INPUT, outputType, FilterType.of(INPUT, StringType.getInstance()))));
		this.typeSchemes = binary ? List.of(List.of(), withPathOnly) : List.of(List.of(), withPathOnly,
				List.of(TypeScheme.of(Map.of(INPUT, AnyType.getInstance()), FunctionType.of(INPUT, outputType, FilterType.of(INPUT, StringType.getInstance()), FilterType.of(INPUT, OPTIONS)))));
	}

	@Override
	public List<TypeScheme<FunctionType>> types(Version jqVersion, int totalArguments) {
		if (totalArguments < 1 || totalArguments >= typeSchemes.size())
			return List.of();
		return typeSchemes.get(totalArguments);
	}

	public static FileReadFunction text() {
		return new FileReadFunction(false);
	}

	public static FileReadFunction binary() {
		return new FileReadFunction(true);
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
		boolean binarySupported = binary && supportsBinary(jsonProvider);
		return new Expression<>() {


			@Override
			public void apply(Context context, JsonNode input, Path<JsonNode> inputPath, Output<JsonNode> output) throws JsonQueryException {
				pathExpression.apply(context, input, inputPath, (pathNode, pathPath) -> {
					java.nio.file.Path file = FileFunctionSupport.parsePath(jsonProvider, pathNode, functionName());
					if (binary) {
						output.emit(readBinary(jsonProvider, context.getRuntimeLimits(), file, binarySupported), UntrackedPath.getInstance());
						return;
					}
					if (optionsExpression == null) {
						output.emit(readText(jsonProvider, context.getRuntimeLimits(), file, StandardCharsets.UTF_8), UntrackedPath.getInstance());
						return;
					}
					optionsExpression.apply(context, input, inputPath, (optionsNode, optionsPath) -> {
						Charset charset = FileFunctionSupport.parseCharset(jsonProvider, optionsNode, functionName());
						output.emit(readText(jsonProvider, context.getRuntimeLimits(), file, charset), UntrackedPath.getInstance());
					});
				});
			}
		};
	}

	private String functionName() {
		return binary ? "fs::read_binary" : "fs::read_text";
	}

	private static <JsonNode> boolean supportsBinary(JsonProvider<JsonNode> jsonProvider) {
		try {
			jsonProvider.createBinary(new byte[0]);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	private static <JsonNode> JsonNode readText(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, java.nio.file.Path file, Charset charset) {
		StringBuilder result = new StringBuilder();
		try (Reader reader = new InputStreamReader(Files.newInputStream(file), charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT))) {
			char[] buffer = new char[8192];
			for (int count; (count = reader.read(buffer)) != -1; ) {
				RuntimeLimitChecks.checkStringLength(limits, (long) result.length() + count);
				result.append(buffer, 0, count);
			}
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException("fs::read_text failed for " + file + " using " + charset.name() + ": " + e.getMessage(), e);
		}
		return jsonProvider.createString(result.toString());
	}

	private static <JsonNode> JsonNode readBinary(JsonProvider<JsonNode> jsonProvider, RuntimeLimits limits, java.nio.file.Path file, boolean binarySupported) {
		int maximumBytes = binarySupported ? limits.getMaxBinaryLength() : RuntimeLimitChecks.maximumBytesForBase64(limits.getMaxStringLength());
		LimitedByteArrayOutputStream result = new LimitedByteArrayOutputStream(maximumBytes, binarySupported, limits.getMaxStringLength());
		try (InputStream input = Files.newInputStream(file)) {
			byte[] buffer = new byte[8192];
			for (int count; (count = input.read(buffer)) != -1; )
				result.write(buffer, 0, count);
		} catch (IOException | SecurityException e) {
			throw new JsonQueryException("fs::read_binary failed for " + file + ": " + e.getMessage(), e);
		}
		byte[] bytes = result.toByteArray();
		if (binarySupported)
			return jsonProvider.createBinary(bytes);
		return jsonProvider.createString(Base64.getEncoder().encodeToString(bytes));
	}

	private static final class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
		private final boolean binary;
		private final int maximumBytes;
		private final int maximumStringLength;

		private LimitedByteArrayOutputStream(int maximumBytes, boolean binary, int maximumStringLength) {
			this.maximumBytes = maximumBytes;
			this.binary = binary;
			this.maximumStringLength = maximumStringLength;
		}

		@Override
		public synchronized void write(int value) {
			checkLength((long) count + 1);
			super.write(value);
		}

		@Override
		public synchronized void write(byte[] bytes, int offset, int length) {
			checkLength((long) count + length);
			super.write(bytes, offset, length);
		}

		private void checkLength(long length) {
			if (length <= maximumBytes)
				return;
			if (binary)
				throw new RuntimeLimitExceededException("Binary value of " + length + " bytes exceeds the maximum binary length of " + maximumBytes);
			long encodedLength = 4 * ((length + 2) / 3);
			throw new RuntimeLimitExceededException("String of " + encodedLength + " characters exceeds the maximum string length of " + maximumStringLength);
		}
	}
}
