import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.core {
	requires static com.google.auto.service;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.core;
	exports net.thisptr.jackson.jq.v2.core.diagnostic;
	exports net.thisptr.jackson.jq.v2.core.function;
	exports net.thisptr.jackson.jq.v2.core.function.loaders;
	exports net.thisptr.jackson.jq.v2.core.module;
	exports net.thisptr.jackson.jq.v2.core.module.loaders;
	exports net.thisptr.jackson.jq.v2.core.version;

	opens net.thisptr.jackson.jq.v2.core.internal.commons.pair to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.commons.range to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.compile to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.compile.freevars to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.misc to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.literal to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.matcher to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.matcher.matchers to net.thisptr.jackson.jq.v2.ext.module.debug;

	uses net.thisptr.jackson.jq.v2.spi.Function;
	uses net.thisptr.jackson.jq.v2.spi.JqLibrary;
	uses net.thisptr.jackson.jq.v2.spi.module.Module;

	provides net.thisptr.jackson.jq.v2.spi.JqLibrary with
			net.thisptr.jackson.jq.v2.core.internal.builtins.library.CoreJqLibrary;

	provides net.thisptr.jackson.jq.v2.spi.Function with
			net.thisptr.jackson.jq.v2.core.internal.builtins.filters.CsvFilter,
			net.thisptr.jackson.jq.v2.core.internal.builtins.filters.TsvFilter,
			net.thisptr.jackson.jq.v2.core.internal.builtins.AtBase64dFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.AtBase64Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.AtHtmlFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.AtShFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.AtUriFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.BuiltinsFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ContainsFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.DelPathsFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.EmptyFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.EndsWithFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ErrorFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ExplodeFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.FromDateIso8601Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.FromEntriesFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.FromJsonFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.GetPathFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.GroupByFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.HasFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ImplodeFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IndexFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IndicesFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.InfiniteFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IsEmptyFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IsInfiniteFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IsNanFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.IsNormalFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.JoinFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.KeysFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.KeysUnsortedFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.LengthFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.LTrimStrFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.Atan2Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.AcosFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.AsinFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.AtanFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.CbrtFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.CeilFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.CosFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.CoshFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Exp10Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Exp2Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.ExpFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Expm1Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.FloorFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Log10Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Log1pFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.Log2Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.LogFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.RoundFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.SinFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.SinhFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.SqrtFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.TanFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.MathFunctions.TanhFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.math.PowFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.MaxByFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.MinByFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.NanFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.NotFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.NowFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.PathFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.PathsFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.RangeFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ReverseFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.RIndexFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.RTrimStrFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.SetPathFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.SortByFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.SplitFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.StartsWithFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ToDateIso8601Function,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ToEntriesFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ToJsonFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ToNumberFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.ToStringFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.TypeFunction,
			net.thisptr.jackson.jq.v2.core.internal.builtins.Utf8ByteLengthFunction;
}
