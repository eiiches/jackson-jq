package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;

public class TypeFilterFunctions {
	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "arrays", nargs = 0)
	public static class ArraysFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.ARRAY) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "booleans", nargs = 0)
	public static class BooleansFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.BOOLEAN) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "objects", nargs = 0)
	public static class ObjectsFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.OBJECT) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "numbers", nargs = 0)
	public static class NumbersFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.NUMBER) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "strings", nargs = 0)
	public static class StringsFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.STRING || jsonProvider.getNodeType(in) == JsonNodeType.BINARY) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "nulls", nargs = 0)
	public static class NullsFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) == JsonNodeType.NULL) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "values", nargs = 0)
	public static class ValuesFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				if (jsonProvider.getNodeType(in) != JsonNodeType.NULL) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "iterables", nargs = 0)
	public static class IterablesFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				JsonNodeType type = jsonProvider.getNodeType(in);
				if (type == JsonNodeType.ARRAY || type == JsonNodeType.OBJECT) {
					output.emit(in, path);
				}
			};
		}
	}

	@AutoService(FunctionFactory.class)
	@FunctionRegistration(name = "scalars", nargs = 0)
	public static class ScalarsFunction implements FunctionFactory {
		@Override
		public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version version) {
			return (scope, in, path, output) -> {
				JsonNodeType type = jsonProvider.getNodeType(in);
				if (type == JsonNodeType.NULL || type == JsonNodeType.BOOLEAN || type == JsonNodeType.NUMBER || type == JsonNodeType.STRING || type == JsonNodeType.BINARY) {
					output.emit(in, path);
				}
			};
		}
	}
}
