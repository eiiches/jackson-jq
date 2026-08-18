import net.thisptr.jackson.jq.v2.spi.Function;

module net.thisptr.jackson.jq.v2.core {
	requires static com.google.auto.service;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static org.jspecify;

	exports net.thisptr.jackson.jq.v2.core;
	exports net.thisptr.jackson.jq.v2.core.exception;
	exports net.thisptr.jackson.jq.v2.core.module.loaders;
	exports net.thisptr.jackson.jq.v2.core.path;

	uses Function;
	uses net.thisptr.jackson.jq.v2.spi.JqLibrary;
	uses net.thisptr.jackson.jq.v2.spi.module.Module;

	provides net.thisptr.jackson.jq.v2.spi.JqLibrary with
		net.thisptr.jackson.jq.v2.core.internal.CoreJqLibrary;

	provides Function with
		net.thisptr.jackson.jq.v2.core.internal.filters.CsvFilter,
		net.thisptr.jackson.jq.v2.core.internal.filters.TsvFilter,
		net.thisptr.jackson.jq.v2.core.internal.functions.AtBase64dFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.AtBase64Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.AtHtmlFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.AtShFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.AtUriFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.BuiltinsFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ContainsFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.DelPathsFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.EmptyFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.EndsWithFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.EnvFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ErrorFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ExplodeFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.FromDateIso8601Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.FromEntriesFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.FromJsonFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.GetPathFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.GroupByFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.HasFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ImplodeFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.IndexFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.IndicesFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.InfiniteFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.IsInfiniteFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.IsNanFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.IsNormalFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.JoinFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.KeysFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.KeysUnsortedFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.LengthFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.LTrimStrFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.math.Atan2Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.AcosFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.AsinFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.AtanFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.CbrtFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.CeilFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.CosFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.CoshFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Exp10Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Exp2Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.ExpFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Expm1Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.FloorFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Log10Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Log1pFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.Log2Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.LogFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.RoundFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.SinFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.SinhFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.SqrtFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.TanFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MathFunction.TanhFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.math.PowFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MaxByFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.MinByFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.NanFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.NotFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.NowFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.PathFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.PathsFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.RangeFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ReverseFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.RIndexFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.RTrimStrFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.SetPathFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.SortByFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.SplitFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.StartsWithFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ToDateIso8601Function,
		net.thisptr.jackson.jq.v2.core.internal.functions.ToEntriesFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ToJsonFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ToNumberFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.ToStringFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.TypeFunction,
		net.thisptr.jackson.jq.v2.core.internal.functions.Utf8ByteLengthFunction;
}
